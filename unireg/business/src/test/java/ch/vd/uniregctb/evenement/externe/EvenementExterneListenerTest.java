package ch.vd.uniregctb.evenement.externe;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.Assert;

import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.ListeType;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.OrigineType;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.QuittanceType;
import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
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

public class EvenementExterneListenerTest extends BusinessTest {

	private EvenementExterneListenerImpl listener;

	private EvenementExterneDAO evenementExterneDAO;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementExterneDAO = getBean(EvenementExterneDAO.class, "evenementExterneDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final EvenementExterneService service = getBean(EvenementExterneService.class, "evenementExterneService");

		listener = new EvenementExterneListenerImpl();
		listener.setHandler(service);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEventImpotSource() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // l'état "EMISE" et l'état "RETOURNEE"

				final EtatDeclaration etatEmission = lr.getEtatDeclarationActif(TypeEtatDeclaration.EMISE);
				assertNotNull(etatEmission);
				assertTrue(etats.contains(etatEmission));
				assertEquals(TypeEtatDeclaration.EMISE, etatEmission.getEtat());
				assertEquals(dateFin, etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = lr.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
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
	public void testEventLC() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageLC(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(0, evts.size());
				

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEventAnnulationEtatRetourInexistant() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
	public void testEventDoubleAnnulation() throws Exception{

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
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

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
	public void testEventAnnulation() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
	public void testEvenementDoubleQuittancement() throws Exception {
		
		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
	public void testEvenementAnnulationDoubleQuittancement() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
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

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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

	private String createMessageQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) {
		final EvtQuittanceListeDocument doc = createEvenementQuittancement(QuittanceType.QUITTANCEMENT, noCtb, ListeType.LR, debutPeriode, finPeriode, dateEvenement);
		return doc.xmlText();
	}
	
	private String createMessageAnnulationQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) {
		final EvtQuittanceListeDocument doc = createEvenementQuittancement(QuittanceType.ANNULATION, noCtb, ListeType.LR, debutPeriode, finPeriode, dateEvenement);
		return doc.xmlText();
	}

	private String createMessageLC(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) {
		final EvtQuittanceListeDocument doc = createEvenementQuittancement(QuittanceType.QUITTANCEMENT, noCtb, ListeType.LC, debutPeriode, finPeriode, dateEvenement);
		return doc.xmlText();
	}

	private static EvtQuittanceListeDocument createEvenementQuittancement(QuittanceType.Enum quitancement, Long numeroCtb, ListeType.Enum listeType, RegDate dateDebut,
	                                                              RegDate dateFin, RegDate dateEvenement) {

		Assert.notNull(quitancement, "le type de quittancement est obligation");
		Assert.notNull(numeroCtb, "Le numero du débiteur est obligatoire");
		Assert.notNull(dateDebut, "la date du début du récapitulatif est obligatoire");
		// Assert.assertNotNull(dateFin);

		final EvtQuittanceListeDocument doc = EvtQuittanceListeDocument.Factory.newInstance();
		final EvtQuittanceListeDocument.EvtQuittanceListe evenement = doc.addNewEvtQuittanceListe();
		final EvtQuittanceListeDocument.EvtQuittanceListe.IdentificationListe identification = evenement.addNewIdentificationListe();
		identification.setNumeroDebiteur(numeroCtb.intValue());
		final EvtQuittanceListeDocument.EvtQuittanceListe.IdentificationListe.PeriodeDeclaration periodeDeclaration = identification.addNewPeriodeDeclaration();
		final Calendar datedebutC = DateUtils.calendar(dateDebut.asJavaDate());
		periodeDeclaration.setDateDebut(datedebutC);
		if (dateFin != null) {
			final Calendar dateFinC = DateUtils.calendar(dateFin.asJavaDate());
			periodeDeclaration.setDateFin(dateFinC);
		}
		identification.setPeriodeDeclaration(periodeDeclaration);
		identification.setTypeListe(listeType);
		identification.setNumeroSequence(new BigInteger("1"));
		evenement.setIdentificationListe(identification);
		evenement.setTypeEvtQuittance(quitancement);
		evenement.setOrigineListe(OrigineType.ELECTRONIQUE);
		Assert.notNull(dateEvenement, "la date de quittancement du récapitulatif est obligatoire");
		evenement.setTimestampEvtQuittance(DateUtils.calendar(dateEvenement.asJavaDate()));
		doc.setEvtQuittanceListe(evenement);

		return doc;
	}
}
