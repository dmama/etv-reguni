package ch.vd.uniregctb.evenement.externe;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigInteger;
import java.util.Arrays;
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
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EvenementExterneOldV1EsbHandlerTest extends BusinessTest {

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

		final List<EvenementExterneConnector> connectors = Arrays.<EvenementExterneConnector>asList(new EvtQuittanceListeV1Connector(), new EvtListeV1Connector(), new EvtListeV2Connector(), new EvtListeV3Connector());
		handler.setConnectors(connectors);
		handler.afterPropertiesSet();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOldEventImpotSource() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

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

				final Set<EtatDeclaration> etats = lr.getEtatsDeclaration();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // l'état "EMIS" et l'état "RETOURNE"

				final EtatDeclaration etatEmission = lr.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMIS);
				assertNotNull(etatEmission);
				assertTrue(etats.contains(etatEmission));
				assertEquals(TypeEtatDocumentFiscal.EMIS, etatEmission.getEtat());
				assertEquals(dateFin, etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = lr.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE);
				assertNotNull(etatRetour);
				assertTrue(etats.contains(etatRetour));
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etatRetour.getEtat());
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
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

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
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

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

				final Set<EtatDeclaration> etats = lr.getEtatsDeclaration();
				assertNotNull(etats);
				assertEquals(1, etats.size());      // l'état "EMISE"

				final EtatDeclaration etatEmission = etats.iterator().next();
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMIS, etatEmission.getEtat());
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
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
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
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
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

				final List<EtatDeclaration> etats = lr.getEtatsDeclarationSorted();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // états "EMIS" et "RETOURNE" (ce dernier annulé)

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMIS, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = etats.get(1);
				assertNotNull(etatRetour);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etatRetour.getEtat());
				assertEquals(dateQuittancement, etatRetour.getDateObtention());
				assertTrue(etatRetour.isAnnule());

				return null;
			}
		});
	}

	@Test
	public void testOldEvenementDoubleQuittancement() throws Exception {
		
		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
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

				final List<EtatDeclaration> etats = lr.getEtatsDeclarationSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());      // l'état "EMIS", puis les deux états "RETOURNE", dont l'un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMIS, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatAnnule = etats.get(1);
				assertNotNull(etatAnnule);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etatAnnule.getEtat());
				assertEquals(dateQuittancement, etatAnnule.getDateObtention());
				assertTrue(etatAnnule.isAnnule());

				final EtatDeclaration etatValide = etats.get(2);
				assertNotNull(etatValide);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etatValide.getEtat());
				assertEquals(dateQuittancement, etatValide.getDateObtention());
				assertFalse(etatValide.isAnnule());

				final EtatDeclaration dernierEtat = lr.getDernierEtatDeclaration();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testOldEvenementAnnulationDoubleQuittancement() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
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

				final List<EtatDeclaration> etats = lr.getEtatsDeclarationSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());      // "EMIS", et deux "RETOURNE", dont un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMIS, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatAnnule = etats.get(1);
				assertNotNull(etatAnnule);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etatAnnule.getEtat());
				assertEquals(dateQuittancement, etatAnnule.getDateObtention());
				assertTrue(etatAnnule.isAnnule());

				final EtatDeclaration etatValide = etats.get(2);
				assertNotNull(etatValide);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etatValide.getEtat());
				assertEquals(dateQuittancement, etatValide.getDateObtention());
				assertFalse(etatValide.isAnnule());

				final EtatDeclaration dernierEtat = lr.getDernierEtatDeclaration();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, dernierEtat.getEtat());
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

		final EsbMessage m = EsbMessageFactory.createMessage();
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
}
