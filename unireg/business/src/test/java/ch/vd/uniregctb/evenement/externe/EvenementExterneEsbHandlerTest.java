package ch.vd.uniregctb.evenement.externe;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.event.lr.event.v1.CaracteristiquesDebiteur;
import ch.vd.unireg.xml.event.lr.event.v1.CaracteristiquesListe;
import ch.vd.unireg.xml.event.lr.event.v1.DebiteurQualification;
import ch.vd.unireg.xml.event.lr.event.v1.Evenement;
import ch.vd.unireg.xml.event.lr.event.v1.EvtListe;
import ch.vd.unireg.xml.event.lr.event.v1.PeriodeDeclaration;
import ch.vd.unireg.xml.event.lr.event.v1.Utilisateur;
import ch.vd.unireg.xml.event.lr.quittance.v1.EvtQuittanceListe;
import ch.vd.unireg.xml.event.lr.quittance.v1.Liste;
import ch.vd.unireg.xml.event.lr.quittance.v1.ObjectFactory;
import ch.vd.unireg.xml.event.lr.quittance.v1.Origine;
import ch.vd.unireg.xml.event.lr.quittance.v1.Quittance;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EvenementExterneEsbHandlerTest extends BusinessTest {

	private EvenementExterneEsbHandler handler;

	private EvenementExterneDAO evenementExterneDAO;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementExterneDAO = getBean(EvenementExterneDAO.class, "evenementExterneDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final EvenementExterneService service = getBean(EvenementExterneService.class, "evenementExterneService");

		handler = new EvenementExterneEsbHandler();
		handler.setHandler(service);

		final List<EvenementExterneConnector> connectors = Arrays.<EvenementExterneConnector>asList(new EvtQuittanceListeV1Connector(), new EvtListeV1Connector(), new EvtListeV2Connector());
		handler.setConnectors(connectors);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOldEventImpotSource() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createOldMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				final EvenementExterne evt = evts.get(0);
				assertNotNull(evt);
				assertEquals(EtatEvenementExterne.TRAITE, evt.getEtat());
				final String xml = evt.getMessage();
				assertTrue(xml, xml.startsWith("<?xml version=\"1.0\""));
				assertTrue(xml, xml.endsWith("evtQuittanceListe>"));

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // l'état "EMISE" et l'état "RETOURNEE"

				final EtatDeclaration etatEmission = lr.getDernierEtatOfType(TypeEtatDeclaration.EMISE);
				assertNotNull(etatEmission);
				assertTrue(etats.contains(etatEmission));
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin, etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = lr.getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE);
				assertNotNull(etatRetour);
				assertTrue(etats.contains(etatRetour));
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatRetour.getEtat());
				assertEquals(dateQuittancement, etatRetour.getDateObtention());
				assertFalse(etatRetour.isAnnule());

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOldEventLC() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createOldMessageLC(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(0, evts.size());
				

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOldEventAnnulationEtatRetourInexistant() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createOldMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.ERREUR, evts.get(0).getEtat());
				final String erreurAttendue = String.format("La déclaration impôt source sélectionnée (tiers=%d, période=%s) ne contient pas de retour à annuler.",
				                                            dpiId, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(dateDebut, dateFin)));
				assertEquals(erreurAttendue, evts.get(0).getErrorMessage());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(1, etats.size());      // l'état "EMISE"

				final EtatDeclaration etatEmission = etats.iterator().next();
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin, etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOldEventDoubleAnnulation() throws Exception{

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationEmise(dateFin.addDays(-10)));

				final EtatDeclaration etatRetourne = new EtatDeclarationRetournee(dateQuittancement, "TEST");
				etatRetourne.setAnnule(true);
				lr.addEtat(etatRetourne);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createOldMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.ERREUR, evts.get(0).getEtat());
				final String erreurAttendue = String.format("La déclaration impôt source sélectionnée (tiers=%s, période=%s) ne contient pas de retour à annuler.",
				                                            dpiId, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(dateDebut, dateFin)));
				assertEquals(erreurAttendue, evts.get(0).getErrorMessage());
				return null;
			}
		});
	}

	@Test
	public void testOldEventAnnulation() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createOldMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsSorted();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // états "EMISE" et "RETOURNEE" (ce dernier annulé)

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = etats.get(1);
				assertNotNull(etatRetour);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatRetour.getEtat());
				assertEquals(dateQuittancement, etatRetour.getDateObtention());
				assertTrue(etatRetour.isAnnule());

				return null;
			}
		});
	}

	@Test
	public void testOldEvenementDoubleQuittancement() throws Exception {
		
		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createOldMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());      // l'état "EMISE", puis les deux états "RETOURNEE", dont l'un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatAnnule = etats.get(1);
				assertNotNull(etatAnnule);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatAnnule.getEtat());
				assertEquals(dateQuittancement, etatAnnule.getDateObtention());
				assertTrue(etatAnnule.isAnnule());

				final EtatDeclaration etatValide = etats.get(2);
				assertNotNull(etatValide);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatValide.getEtat());
				assertEquals(dateQuittancement, etatValide.getDateObtention());
				assertFalse(etatValide.isAnnule());

				final EtatDeclaration dernierEtat = lr.getDernierEtat();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testOldEvenementAnnulationDoubleQuittancement() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createOldMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());      // "EMISE", et deux "RETOURNEE", dont un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatAnnule = etats.get(1);
				assertNotNull(etatAnnule);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatAnnule.getEtat());
				assertEquals(dateQuittancement, etatAnnule.getDateObtention());
				assertTrue(etatAnnule.isAnnule());

				final EtatDeclaration etatValide = etats.get(2);
				assertNotNull(etatValide);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatValide.getEtat());
				assertEquals(dateQuittancement, etatValide.getDateObtention());
				assertFalse(etatValide.isAnnule());

				final EtatDeclaration dernierEtat = lr.getDernierEtat();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}

	private EsbMessage createOldMessageQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createOldEvenementQuittancement(Quittance.QUITTANCEMENT, noCtb, Liste.LR, debutPeriode, finPeriode, dateEvenement));
	}
	
	private EsbMessage createOldMessageAnnulationQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createOldEvenementQuittancement(Quittance.ANNULATION, noCtb, Liste.LR, debutPeriode, finPeriode, dateEvenement));
	}

	private EsbMessage createOldMessageLC(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createOldEvenementQuittancement(Quittance.QUITTANCEMENT, noCtb, Liste.LC, debutPeriode, finPeriode, dateEvenement));
	}

	private EsbMessage createEsbMessage(EvtQuittanceListe event) throws Exception {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.newDocument();

		final QName qname = new QName("http://www.vd.ch/fiscalite/taxation/evtQuittanceListe-v1", "evtQuittanceListe");
		marshaller.marshal(new JAXBElement<>(qname, EvtQuittanceListe.class, event), doc);

		final EsbMessage m = new EsbMessageFactory().createMessage();
		m.setBody(doc);

		return m;
	}

	private static EvtQuittanceListe createOldEvenementQuittancement(Quittance quitancement, Long numeroCtb, Liste listeType, RegDate dateDebut,
	                                                                 RegDate dateFin, RegDate dateEvenement) {

		assertNotNull("le type de quittancement est obligation", quitancement);
		assertNotNull("Le numero du débiteur est obligatoire", numeroCtb);
		assertNotNull("la date du début du récapitulatif est obligatoire", dateDebut);

		final EvtQuittanceListe evenement = new EvtQuittanceListe();
		final EvtQuittanceListe.IdentificationListe identification = new EvtQuittanceListe.IdentificationListe();
		identification.setNumeroDebiteur(numeroCtb.intValue());
		final EvtQuittanceListe.IdentificationListe.PeriodeDeclaration periodeDeclaration = new EvtQuittanceListe.IdentificationListe.PeriodeDeclaration();
		periodeDeclaration.setDateDebut(XmlUtils.regdate2xmlcal(dateDebut));
		if (dateFin != null) {
			periodeDeclaration.setDateFin(XmlUtils.regdate2xmlcal(dateFin));
		}
		identification.setPeriodeDeclaration(periodeDeclaration);
		identification.setTypeListe(listeType);
		identification.setNumeroSequence(new BigInteger("1"));
		evenement.setIdentificationListe(identification);
		evenement.setTypeEvtQuittance(quitancement);
		evenement.setOrigineListe(Origine.ELECTRONIQUE);
		assertNotNull("la date de quittancement du récapitulatif est obligatoire", dateEvenement);
		evenement.setTimestampEvtQuittance(XmlUtils.regdate2xmlcal(dateEvenement));

		return evenement;
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNewEventImpotSource() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createNewMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				final EvenementExterne evt = evts.get(0);
				assertNotNull(evt);
				assertEquals(EtatEvenementExterne.TRAITE, evt.getEtat());
				final String xml = evt.getMessage();
				assertTrue(xml, xml.startsWith("<?xml version=\"1.0\""));
				assertTrue(xml, xml.endsWith("evtListe>"));


				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // l'état "EMISE" et l'état "RETOURNEE"

				final EtatDeclaration etatEmission = lr.getDernierEtatOfType(TypeEtatDeclaration.EMISE);
				assertNotNull(etatEmission);
				assertTrue(etats.contains(etatEmission));
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin, etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = lr.getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE);
				assertNotNull(etatRetour);
				assertTrue(etats.contains(etatRetour));
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatRetour.getEtat());
				assertEquals(dateQuittancement, etatRetour.getDateObtention());
				assertFalse(etatRetour.isAnnule());

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNewEventLC() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createNewMessageLC(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(0, evts.size());


				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNewEventAnnulationEtatRetourInexistant() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createNewMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.ERREUR, evts.get(0).getEtat());
				final String erreurAttendue = String.format("La déclaration impôt source sélectionnée (tiers=%d, période=%s) ne contient pas de retour à annuler.",
						dpiId, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(dateDebut, dateFin)));
				assertEquals(erreurAttendue, evts.get(0).getErrorMessage());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(1, etats.size());      // l'état "EMISE"

				final EtatDeclaration etatEmission = etats.iterator().next();
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin, etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNewEventDoubleAnnulation() throws Exception{

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationEmise(dateFin.addDays(-10)));

				final EtatDeclaration etatRetourne = new EtatDeclarationRetournee(dateQuittancement, "TEST");
				etatRetourne.setAnnule(true);
				lr.addEtat(etatRetourne);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createNewMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.ERREUR, evts.get(0).getEtat());
				final String erreurAttendue = String.format("La déclaration impôt source sélectionnée (tiers=%s, période=%s) ne contient pas de retour à annuler.",
						dpiId, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(dateDebut, dateFin)));
				assertEquals(erreurAttendue, evts.get(0).getErrorMessage());
				return null;
			}
		});
	}

	@Test
	public void testNewEventAnnulation() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createNewMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsSorted();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // états "EMISE" et "RETOURNEE" (ce dernier annulé)

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = etats.get(1);
				assertNotNull(etatRetour);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatRetour.getEtat());
				assertEquals(dateQuittancement, etatRetour.getDateObtention());
				assertTrue(etatRetour.isAnnule());

				return null;
			}
		});
	}

	@Test
	public void testNewEvenementDoubleQuittancement() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createNewMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());      // l'état "EMISE", puis les deux états "RETOURNEE", dont l'un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatAnnule = etats.get(1);
				assertNotNull(etatAnnule);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatAnnule.getEtat());
				assertEquals(dateQuittancement, etatAnnule.getDateObtention());
				assertTrue(etatAnnule.isAnnule());

				final EtatDeclaration etatValide = etats.get(2);
				assertNotNull(etatValide);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatValide.getEtat());
				assertEquals(dateQuittancement, etatValide.getDateObtention());
				assertFalse(etatValide.isAnnule());

				final EtatDeclaration dernierEtat = lr.getDernierEtat();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testNewEvenementAnnulationDoubleQuittancement() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = createNewMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());      // "EMISE", et deux "RETOURNEE", dont un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatAnnule = etats.get(1);
				assertNotNull(etatAnnule);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatAnnule.getEtat());
				assertEquals(dateQuittancement, etatAnnule.getDateObtention());
				assertTrue(etatAnnule.isAnnule());

				final EtatDeclaration etatValide = etats.get(2);
				assertNotNull(etatValide);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etatValide.getEtat());
				assertEquals(dateQuittancement, etatValide.getDateObtention());
				assertFalse(etatValide.isAnnule());

				final EtatDeclaration dernierEtat = lr.getDernierEtat();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}

	private EsbMessage createNewMessageQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createNewEvenementQuittancement(Evenement.QUITTANCE, noCtb, ch.vd.unireg.xml.event.lr.event.v1.Liste.LR, debutPeriode, finPeriode, dateEvenement));
	}

	private EsbMessage createNewMessageAnnulationQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createNewEvenementQuittancement(Evenement.ANNULATION, noCtb, ch.vd.unireg.xml.event.lr.event.v1.Liste.LR, debutPeriode, finPeriode, dateEvenement));
	}

	private EsbMessage createNewMessageLC(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createNewEvenementQuittancement(Evenement.QUITTANCE, noCtb, ch.vd.unireg.xml.event.lr.event.v1.Liste.LC, debutPeriode, finPeriode, dateEvenement));
	}

	private static EvtListe createNewEvenementQuittancement(Evenement quitancement, Long numeroCtb, ch.vd.unireg.xml.event.lr.event.v1.Liste listeType, RegDate dateDebut,
	                                                                 RegDate dateFin, RegDate dateEvenement) {

		assertNotNull("le type de quittancement est obligation", quitancement);
		assertNotNull("Le numero du débiteur est obligatoire", numeroCtb);
		assertNotNull("la date du début du récapitulatif est obligatoire", dateDebut);
		assertNotNull("la date de quittancement du récapitulatif est obligatoire", dateEvenement);

		final String businessId = "123456789";

		final PeriodeDeclaration periodeDeclaration = new PeriodeDeclaration();
		periodeDeclaration.setDateDebut(XmlUtils.regdate2xmlcal(dateDebut));
		if (dateFin != null) {
			periodeDeclaration.setDateFin(XmlUtils.regdate2xmlcal(dateFin));
		}

		final CaracteristiquesListe identification = new CaracteristiquesListe();
		identification.setTypeListe(listeType);
		identification.setNumeroSequence(BigInteger.valueOf(1));
		identification.setPeriodeDeclaration(periodeDeclaration);
		identification.setPeriodeFiscale(BigInteger.valueOf(dateDebut.year()));

		final CaracteristiquesDebiteur debiteur = new CaracteristiquesDebiteur();
		debiteur.setNumeroDebiteur(numeroCtb.intValue());
		debiteur.setTypeDebiteur(DebiteurQualification.REG);

		final EvtListe evenement = new EvtListe();
		evenement.setUtilisateur(new Utilisateur("testuser", 22));
		evenement.setCaracteristiquesListe(identification);
		evenement.setTypeEvenement(quitancement);
		evenement.setCaracteristiquesDebiteur(debiteur);
		evenement.setCodeApplication("test");
		evenement.setVersionApplication("0.0");
		evenement.setTimestamp(XmlUtils.date2xmlcal(new Date()));
		evenement.setBusinessId(businessId);
		evenement.setDateEvenement(XmlUtils.regdate2xmlcal(dateEvenement));

		return evenement;
	}

	private EsbMessage createEsbMessage(EvtListe event) throws Exception {
		JAXBContext context = JAXBContext.newInstance(ch.vd.unireg.xml.event.lr.event.v1.ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.newDocument();

		final QName qname = new QName("http://www.vd.ch/fiscalite/taxation/is/evt-liste/1", "evtListe");
		marshaller.marshal(new JAXBElement<>(qname, EvtListe.class, event), doc);

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBody(doc);

		return m;
	}
}
