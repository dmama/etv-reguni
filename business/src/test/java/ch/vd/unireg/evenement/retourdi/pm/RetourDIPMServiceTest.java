package ch.vd.unireg.evenement.retourdi.pm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseMandataireSuisse;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.BusinessTestingConstants;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.NumeroIDEHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.tiers.TacheControleDossier;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeMandat;
import ch.vd.unireg.type.TypeTache;

@SuppressWarnings("Duplicates")
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES,     // on a besoin d'une véritable synchro sur les tâches...
})
public class RetourDIPMServiceTest extends BusinessTest {

	private RetourDIPMService service;
	private ParametreAppService parametreAppService;
	private TacheDAO tacheDAO;

	public RetourDIPMServiceTest() {
		setWantSynchroTache(true);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		// petit passage par l'indexation des tiers activée pour vider l'indexeur en entrée
		setWantIndexationTiers(true);
		try {
			super.runOnSetUp();
			service = getBean(RetourDIPMService.class, "retourDIPMService");
			parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
			tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		}
		finally {
			setWantIndexationTiers(false);
		}
	}

	/**
	 * Petite classe de callback qui permet de temporairement placer la première année d'envoi des DI PM
	 * à une valeur donnée (en production, c'est 2016, ce qui ne laisse, à l'heure où ces lignes sont écrites,
	 * que peu de place pour les tests...)
	 */
	private class ChangementPremiereAnneeDeclarationPMInitCleanupCallback implements InitCleanupCallback {

		private final int premiereAnneeVoulue;
		private Integer oldPremiereAnnee;

		public ChangementPremiereAnneeDeclarationPMInitCleanupCallback(int premiereAnneeVoulue) {
			this.premiereAnneeVoulue = premiereAnneeVoulue;
		}

		@Override
		public void init() throws Exception {
			oldPremiereAnnee = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(premiereAnneeVoulue);
		}

		@Override
		public void cleanup() throws Exception {
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(oldPremiereAnnee);
		}
	}

	@Test
	public void testRetourSurContribuableInexistant() throws Exception {

		// pas de mise en place fiscale -> personne

		// réception des données de retour
		final RetourDI retour = new RetourDI(42L, 2015, 1, null, null);

		// traitement de ces données
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus transactionStatus) throws Exception {
					service.traiterRetour(retour, Collections.emptyMap());
				}
			});
			Assert.fail("La transaction ne doit pas pouvoir avoir été committée normalement, puisque le tiers 42 n'existe pas...");
		}
		catch (EsbBusinessException e) {
			Assert.assertEquals(EsbBusinessCode.CTB_INEXISTANT, e.getCode());
			Assert.assertEquals("Le contribuable 42 n'existe pas ou n'est pas une entreprise.", e.getMessage());
		}
	}

	@Test
	public void testRetourSurContribuableNonEntreprise() throws Exception {

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("André", "Malraux", date(1901, 11, 3), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		// réception des données de retour
		final RetourDI retour = new RetourDI(id, 2015, 1, null, null);

		// traitement de ces données
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus transactionStatus) throws Exception {
					service.traiterRetour(retour, Collections.emptyMap());
				}
			});
			Assert.fail("La transaction ne doit pas pouvoir avoir été committée normalement, puisque le tiers donné n'est pas une entreprise...");
		}
		catch (EsbBusinessException e) {
			Assert.assertEquals(EsbBusinessCode.CTB_INEXISTANT, e.getCode());
			Assert.assertEquals(String.format("Le contribuable %s n'existe pas ou n'est pas une entreprise.", FormatNumeroHelper.numeroCTBToDisplay(id)), e.getMessage());
		}
	}

	@Test
	public void testRetourSurDeclarationInexistante() throws Exception {

		final RegDate dateDebutEntreprise = date(2015, 2, 1);

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(3, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);
				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final RetourDI retour = new RetourDI(id, 2015, 18, null, null);

		// traitement de ces données
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus transactionStatus) throws Exception {
					service.traiterRetour(retour, Collections.emptyMap());
				}
			});
			Assert.fail("La transaction ne doit pas pouvoir avoir été committée normalement, puisque la déclaration n'existe pas...");
		}
		catch (EsbBusinessException e) {
			Assert.assertEquals(EsbBusinessCode.DECLARATION_ABSENTE, e.getCode());
			Assert.assertEquals(String.format("L'entreprise %s ne possède pas de déclaration d'impôt 2015 avec le numéro de séquence 18.", FormatNumeroHelper.numeroCTBToDisplay(id)), e.getMessage());
		}
	}

	@Test
	public void testRetourSurDeclarationNonQuittancee() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(annee - 1, 2, 1);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 6, 30);

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(3, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 4, 1), date(annee, 3, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 4, 12));

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(id, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// on n'a rien fait, juste une remarque...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(1, entreprise.getBouclements().size());

				// déclaration inchangée
				final List<DeclarationImpotOrdinairePM> allDeclarations = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true);
				Assert.assertEquals(1, allDeclarations.size());
				final DeclarationImpotOrdinairePM declaration = allDeclarations.get(0);
				Assert.assertFalse(declaration.isAnnule());
				Assert.assertEquals(date(annee - 1, 4, 1), declaration.getDateDebut());
				Assert.assertEquals(date(annee - 1, 4, 1), declaration.getDateDebutExerciceCommercial());
				Assert.assertEquals(date(annee, 3, 31), declaration.getDateFin());
				Assert.assertEquals(date(annee, 3, 31), declaration.getDateFinExerciceCommercial());
				Assert.assertEquals((Integer) annee, declaration.getPeriode().getAnnee());

				// remarque
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals(String.format("Données de DI reçues sur la déclaration non-quittancée %d/1 :\n- date de fin d'exercice commercial : %s",
				                                  annee,
				                                  RegDateHelper.dateToDisplayString(nouvelleFinExerciceCommercial)),
				                    remarque.getTexte());

				// tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(id);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Déclaration non-quittancée", tache.getCommentaire());
			}
		});
	}

	@Test
	public void testRetourSurDeclarationAnnulée() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(annee - 1, 2, 1);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 6, 30);

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(3, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 4, 1), date(annee, 3, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 4, 12));
				addEtatDeclarationRetournee(di, date(annee, 7, 31));
				di.setAnnule(true);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(id, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// on n'a rien fait, juste une remarque...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(1, entreprise.getBouclements().size());

				// déclaration inchangée
				final List<DeclarationImpotOrdinairePM> allDeclarations = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true);
				Assert.assertEquals(1, allDeclarations.size());
				final DeclarationImpotOrdinairePM declaration = allDeclarations.get(0);
				Assert.assertTrue(declaration.isAnnule());
				Assert.assertEquals(date(annee - 1, 4, 1), declaration.getDateDebut());
				Assert.assertEquals(date(annee - 1, 4, 1), declaration.getDateDebutExerciceCommercial());
				Assert.assertEquals(date(annee, 3, 31), declaration.getDateFin());
				Assert.assertEquals(date(annee, 3, 31), declaration.getDateFinExerciceCommercial());
				Assert.assertEquals((Integer) annee, declaration.getPeriode().getAnnee());

				// remarque
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals(String.format("Données de DI reçues sur la déclaration annulée %d/1 :\n- date de fin d'exercice commercial : %s",
				                                  annee,
				                                  RegDateHelper.dateToDisplayString(nouvelleFinExerciceCommercial)),
				                    remarque.getTexte());

				// tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(id);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Déclaration annulée", tache.getCommentaire());
			}
		});
	}

	@Test
	public void testRetourSurDeclarationQuittanceeSansChangementExerciceCommercial() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(annee - 1, 2, 1);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 3, 31);

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(3, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 4, 1), nouvelleFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 4, 12));
				addEtatDeclarationRetournee(di, date(annee, 7, 31));

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(id, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// on n'a rien fait, juste une remarque...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(1, entreprise.getBouclements().size());

				// déclaration inchangée
				final List<DeclarationImpotOrdinairePM> allDeclarations = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true);
				Assert.assertEquals(1, allDeclarations.size());
				final DeclarationImpotOrdinairePM declaration = allDeclarations.get(0);
				Assert.assertFalse(declaration.isAnnule());
				Assert.assertEquals(date(annee - 1, 4, 1), declaration.getDateDebut());
				Assert.assertEquals(date(annee - 1, 4, 1), declaration.getDateDebutExerciceCommercial());
				Assert.assertEquals(date(annee, 3, 31), declaration.getDateFin());
				Assert.assertEquals(date(annee, 3, 31), declaration.getDateFinExerciceCommercial());
				Assert.assertEquals((Integer) annee, declaration.getPeriode().getAnnee());

				// remarque -> rien
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// tâche de contrôle de dossier -> aucune
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(id);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialMemeAnneePlusTard() throws Exception {

		final RegDate today = RegDate.get();
		final int annee = today.year() - 1;
		final RegDate dateDebutEntreprise = date(annee - 1, 6, 1);
		final RegDate ancienneFinExerciceCommercial = date(annee, 3, 31);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 6, 30);      // même année en repoussant

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(3, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
				addEtatDeclarationRetournee(di, nouvelleFinExerciceCommercial.addDays(12));

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résulat...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(Collections.emptySet(), entreprise.getRemarques());

				// OK, si on est après le 30.06 de l'année courante, alors une nouvelle tâche va apparaître car la période de l'année
				// courante est maintenant échue
				final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
				if (DayMonth.get(today).compareTo(DayMonth.get(6, 30)) > 0) {
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());
					Assert.assertNull(tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
					final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(today.year() - 1, 7, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(today.year() - 1, 7, 1), tacheEnvoi.getDateDebutExercice());
					Assert.assertEquals(date(today.year(), 6, 30), tacheEnvoi.getDateFin());
					Assert.assertEquals(date(today.year(), 6, 30), tacheEnvoi.getDateFinExercice());
				}
				else {
					Assert.assertEquals(Collections.emptyList(), taches);
				}

				// la déclaration
				final List<DeclarationImpotOrdinairePM> dis = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee, true);
				Assert.assertNotNull(dis);
				Assert.assertEquals(1, dis.size());
				final DeclarationImpotOrdinairePM di = dis.get(0);
				Assert.assertFalse(di.isAnnule());
				Assert.assertEquals((Integer) annee, di.getPeriode().getAnnee());
				Assert.assertEquals(dateDebutEntreprise, di.getDateDebutExerciceCommercial());
				Assert.assertEquals(dateDebutEntreprise, di.getDateDebut());
				Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFinExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFin());

				// les bouclements
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare));
				Assert.assertEquals(2, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(annee, 6, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialMemeAnneePlusTot() throws Exception {

		final int annee = RegDate.get().year() - 2;
		final RegDate dateDebutEntreprise = date(annee - 1, 6, 1);
		final RegDate ancienneFinExerciceCommercial = date(annee, 6, 30);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 3, 31);      // même année en avançant

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, ancienneFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, nouvelleFinExerciceCommercial.addYears(1), MotifFor.FIN_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
				addEtatDeclarationRetournee(di, ancienneFinExerciceCommercial.addDays(20));

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résulat...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(Collections.emptySet(), entreprise.getRemarques());

				final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
				// oui, une tâche a été créée, parce que la période se temine justement au 31.03, qui est passé
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals(TypeTache.TacheEnvoiDeclarationImpotPM, tache.getTypeTache());

				final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
				Assert.assertEquals(date(annee, 4, 1), tacheEnvoi.getDateDebut());
				Assert.assertEquals(date(annee, 4, 1), tacheEnvoi.getDateDebutExercice());
				Assert.assertEquals(date(annee + 1, 3, 31), tacheEnvoi.getDateFin());
				Assert.assertEquals(date(annee + 1, 3, 31), tacheEnvoi.getDateFinExercice());

				// la déclaration
				final List<DeclarationImpotOrdinairePM> dis = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee, true);
				Assert.assertNotNull(dis);
				Assert.assertEquals(1, dis.size());
				final DeclarationImpotOrdinairePM di = dis.get(0);
				Assert.assertFalse(di.isAnnule());
				Assert.assertEquals((Integer) annee, di.getPeriode().getAnnee());
				Assert.assertEquals(dateDebutEntreprise, di.getDateDebutExerciceCommercial());
				Assert.assertEquals(dateDebutEntreprise, di.getDateDebut());
				Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFinExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFin());

				// les bouclements
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(2, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(nouvelleFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(ancienneFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialMemeAnneePlusTotAvecDeclarationExistanteNonRetourneeUlterieure() throws Exception {

		final int annee = RegDate.get().year() - 2;
		final RegDate dateDebutEntreprise = date(annee - 1, 6, 1);
		final RegDate ancienneFinExerciceCommercial = date(annee, 6, 30);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 3, 31);      // même année en avançant

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, ancienneFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, nouvelleFinExerciceCommercial.addYears(1), MotifFor.FIN_EXPLOITATION, MockCommune.Echallens);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// la DI qui va revenir
				{
					final PeriodeFiscale pf = addPeriodeFiscale(annee);
					final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, ancienneFinExerciceCommercial.addDays(20));
				}
				// la DI suivante juste envoyée
				{
					final PeriodeFiscale pf = addPeriodeFiscale(annee + 1);
					final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, ancienneFinExerciceCommercial.getOneDayAfter(), ancienneFinExerciceCommercial.addYears(1), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addYears(1).addDays(5));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résulat...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(Collections.emptySet(), entreprise.getRemarques());

				final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
				Assert.assertEquals(Collections.emptyList(), taches);

				// la déclaration retournée
				final List<DeclarationImpotOrdinairePM> disAnneeRetour = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee, true);
				Assert.assertNotNull(disAnneeRetour);
				Assert.assertEquals(1, disAnneeRetour.size());
				final DeclarationImpotOrdinairePM diRetournee = disAnneeRetour.get(0);
				Assert.assertFalse(diRetournee.isAnnule());
				Assert.assertEquals((Integer) annee, diRetournee.getPeriode().getAnnee());
				Assert.assertEquals(dateDebutEntreprise, diRetournee.getDateDebutExerciceCommercial());
				Assert.assertEquals(dateDebutEntreprise, diRetournee.getDateDebut());
				Assert.assertEquals(nouvelleFinExerciceCommercial, diRetournee.getDateFinExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial, diRetournee.getDateFin());

				// la déclaration suivante
				final List<DeclarationImpotOrdinairePM> disAnneeSuivante = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee + 1, true);
				Assert.assertNotNull(disAnneeSuivante);
				Assert.assertEquals(1, disAnneeSuivante.size());
				final DeclarationImpotOrdinairePM diSuivante = disAnneeSuivante.get(0);
				Assert.assertFalse(diSuivante.isAnnule());
				Assert.assertEquals((Integer) (annee + 1), diSuivante.getPeriode().getAnnee());
				Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), diSuivante.getDateDebutExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), diSuivante.getDateDebut());
				Assert.assertEquals(nouvelleFinExerciceCommercial.addYears(1), diSuivante.getDateFinExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial.addYears(1), diSuivante.getDateFin());

				// les bouclements
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(2, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(nouvelleFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(ancienneFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialMemeAnneePlusTardAvecDeclarationExistanteNonRetourneeUlterieure() throws Exception {

		final int annee = RegDate.get().year() - 2;
		final RegDate dateDebutEntreprise = date(annee - 1, 6, 1);
		final RegDate ancienneFinExerciceCommercial = date(annee, 3, 31);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 6, 30);      // même année en repoussant

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, ancienneFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), DayMonth.get(3, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, nouvelleFinExerciceCommercial.addYears(1), MotifFor.FIN_EXPLOITATION, MockCommune.Echallens);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// la DI qui va revenir
				{
					final PeriodeFiscale pf = addPeriodeFiscale(annee);
					final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, ancienneFinExerciceCommercial.addDays(20));
				}
				// la DI suivante juste envoyée
				{
					final PeriodeFiscale pf = addPeriodeFiscale(annee + 1);
					final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, ancienneFinExerciceCommercial.getOneDayAfter(), ancienneFinExerciceCommercial.addYears(1), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addYears(1).addDays(5));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résulat...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(Collections.emptySet(), entreprise.getRemarques());

				final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
				Assert.assertEquals(Collections.emptyList(), taches);

				// la déclaration retournée
				final List<DeclarationImpotOrdinairePM> disAnneeRetour = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee, true);
				Assert.assertNotNull(disAnneeRetour);
				Assert.assertEquals(1, disAnneeRetour.size());
				final DeclarationImpotOrdinairePM diRetournee = disAnneeRetour.get(0);
				Assert.assertFalse(diRetournee.isAnnule());
				Assert.assertEquals((Integer) annee, diRetournee.getPeriode().getAnnee());
				Assert.assertEquals(dateDebutEntreprise, diRetournee.getDateDebutExerciceCommercial());
				Assert.assertEquals(dateDebutEntreprise, diRetournee.getDateDebut());
				Assert.assertEquals(nouvelleFinExerciceCommercial, diRetournee.getDateFinExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial, diRetournee.getDateFin());

				// la déclaration suivante
				final List<DeclarationImpotOrdinairePM> disAnneeSuivante = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee + 1, true);
				Assert.assertNotNull(disAnneeSuivante);
				Assert.assertEquals(1, disAnneeSuivante.size());
				final DeclarationImpotOrdinairePM diSuivante = disAnneeSuivante.get(0);
				Assert.assertFalse(diSuivante.isAnnule());
				Assert.assertEquals((Integer) (annee + 1), diSuivante.getPeriode().getAnnee());
				Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), diSuivante.getDateDebutExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), diSuivante.getDateDebut());
				Assert.assertEquals(nouvelleFinExerciceCommercial.addYears(1), diSuivante.getDateFinExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial.addYears(1), diSuivante.getDateFin());

				// les bouclements
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(2, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(nouvelleFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(ancienneFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialMemeAnneePlusTotAvecDeclarationExistanteUlterieureRetournee() throws Exception {

		final RegDate today = RegDate.get();
		final int annee = today.year() - 2;
		final RegDate dateDebutEntreprise = date(annee - 1, 6, 1);
		final RegDate ancienneFinExerciceCommercial = date(annee, 6, 30);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 3, 31);      // même année en avançant

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, ancienneFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// la DI qui va revenir
				{
					final PeriodeFiscale pf = addPeriodeFiscale(annee);
					final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, ancienneFinExerciceCommercial.addDays(20));
				}
				// la DI suivante également déjà quittancée
				{
					final PeriodeFiscale pf = addPeriodeFiscale(annee + 1);
					final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, ancienneFinExerciceCommercial.getOneDayAfter(), ancienneFinExerciceCommercial.addYears(1), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addYears(1).addDays(5));
					addEtatDeclarationRetournee(di, ancienneFinExerciceCommercial.addYears(1).addDays(20));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résulat...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(Collections.emptySet(), entreprise.getRemarques());

				// OK, si on est après le 30.06 de l'année courante, alors une nouvelle tâche va apparaître car la période de l'année
				// courante est maintenant échue
				final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
				if (DayMonth.get(today).compareTo(DayMonth.get(6, 30)) > 0) {
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());
					Assert.assertNull(tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
					final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(today.year() - 1, 7, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(today.year() - 1, 7, 1), tacheEnvoi.getDateDebutExercice());
					Assert.assertEquals(date(today.year(), 6, 30), tacheEnvoi.getDateFin());
					Assert.assertEquals(date(today.year(), 6, 30), tacheEnvoi.getDateFinExercice());
				}
				else {
					Assert.assertEquals(Collections.emptyList(), taches);
				}

				// la déclaration retournée
				final List<DeclarationImpotOrdinairePM> disAnneeRetour = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee, true);
				Assert.assertNotNull(disAnneeRetour);
				Assert.assertEquals(1, disAnneeRetour.size());
				final DeclarationImpotOrdinairePM diRetournee = disAnneeRetour.get(0);
				Assert.assertFalse(diRetournee.isAnnule());
				Assert.assertEquals((Integer) annee, diRetournee.getPeriode().getAnnee());
				Assert.assertEquals(dateDebutEntreprise, diRetournee.getDateDebutExerciceCommercial());
				Assert.assertEquals(dateDebutEntreprise, diRetournee.getDateDebut());
				Assert.assertEquals(nouvelleFinExerciceCommercial, diRetournee.getDateFinExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial, diRetournee.getDateFin());

				// la déclaration suivante
				final List<DeclarationImpotOrdinairePM> disAnneeSuivante = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee + 1, true);
				Assert.assertNotNull(disAnneeSuivante);
				Assert.assertEquals(1, disAnneeSuivante.size());
				final DeclarationImpotOrdinairePM diSuivante = disAnneeSuivante.get(0);
				Assert.assertFalse(diSuivante.isAnnule());
				Assert.assertEquals((Integer) (annee + 1), diSuivante.getPeriode().getAnnee());
				Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), diSuivante.getDateDebutExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), diSuivante.getDateDebut());
				Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), diSuivante.getDateFinExerciceCommercial());
				Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), diSuivante.getDateFin());

				// les bouclements
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>((o1, o2) -> NullDateBehavior.EARLIEST.compare(o1.getDateDebut(), o2.getDateDebut())));
				Assert.assertEquals(3, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(nouvelleFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
					Assert.assertEquals(15, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(ancienneFinExerciceCommercial.addYears(2).getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(2);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(ancienneFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialMemeAnneePlusTardAvecDeclarationExistanteUlterieureRetournee() throws Exception {

		final int annee = RegDate.get().year() - 2;
		final RegDate dateDebutEntreprise = date(annee - 1, 6, 1);
		final RegDate ancienneFinExerciceCommercial = date(annee, 3, 31);
		final RegDate nouvelleFinExerciceCommercial = date(annee, 6, 30);      // même année en repoussant

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(3, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, ancienneFinExerciceCommercial.addYears(1), MotifFor.FIN_EXPLOITATION, MockCommune.Echallens);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// la DI qui va revenir
				{
					final PeriodeFiscale pf = addPeriodeFiscale(annee);
					final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, ancienneFinExerciceCommercial.addDays(20));
				}
				// la DI suivante également déjà quittancée
				{
					final PeriodeFiscale pf = addPeriodeFiscale(annee + 1);
					final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, ancienneFinExerciceCommercial.getOneDayAfter(), ancienneFinExerciceCommercial.addYears(1), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addYears(1).addDays(5));
					addEtatDeclarationRetournee(di, ancienneFinExerciceCommercial.addYears(1).addDays(20));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résulat...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(Collections.emptySet(), entreprise.getRemarques());

				final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
				Assert.assertEquals(Collections.emptyList(), taches);

				// la déclaration retournée
				final List<DeclarationImpotOrdinairePM> disAnneeRetour = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee, true);
				Assert.assertNotNull(disAnneeRetour);
				Assert.assertEquals(1, disAnneeRetour.size());
				final DeclarationImpotOrdinairePM diRetournee = disAnneeRetour.get(0);
				Assert.assertFalse(diRetournee.isAnnule());
				Assert.assertEquals((Integer) annee, diRetournee.getPeriode().getAnnee());
				Assert.assertEquals(dateDebutEntreprise, diRetournee.getDateDebutExerciceCommercial());
				Assert.assertEquals(dateDebutEntreprise, diRetournee.getDateDebut());
				Assert.assertEquals(nouvelleFinExerciceCommercial, diRetournee.getDateFinExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial, diRetournee.getDateFin());

				// la déclaration suivante
				final List<DeclarationImpotOrdinairePM> disAnneeSuivante = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, annee + 1, true);
				Assert.assertNotNull(disAnneeSuivante);
				Assert.assertEquals(1, disAnneeSuivante.size());
				final DeclarationImpotOrdinairePM diSuivante = disAnneeSuivante.get(0);
				Assert.assertFalse(diSuivante.isAnnule());
				Assert.assertEquals((Integer) (annee + 1), diSuivante.getPeriode().getAnnee());
				Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), diSuivante.getDateDebutExerciceCommercial());
				Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), diSuivante.getDateDebut());
				Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), diSuivante.getDateFinExerciceCommercial());
				Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), diSuivante.getDateFin());

				// les bouclements
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(3, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(nouvelleFinExerciceCommercial.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(9, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(annee + 1, 3, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(2);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialAnneeDifferenteAvant() throws Exception {

		final int anneeInitiale = 2016;
		final int anneeFinale = 2015;
		final RegDate dateDebutEntreprise = date(Math.min(anneeInitiale, anneeFinale), 2, 1);
		final RegDate ancienneFinExerciceCommercial = date(anneeInitiale, 6, 30);
		final RegDate nouvelleFinExerciceCommercial = date(anneeFinale, 12, 31);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pfInitiale = addPeriodeFiscale(anneeInitiale);
				final PeriodeFiscale pfFinale = addPeriodeFiscale(anneeFinale);

				final ModeleDocument mdInitiale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfInitiale);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// déclaration retournée
				{
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfInitiale, date(anneeFinale, 7, 1), ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdInitiale);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, nouvelleFinExerciceCommercial.addDays(12));
				}

				// pour tester le re-calcul du numéro de séquence, plaçons une déclaration sur le début de la nouvelle PF
				{
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfFinale, dateDebutEntreprise, date(anneeFinale, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, null);
					di.setNumero(42);
					addEtatDeclarationEmise(di, date(anneeFinale, 4, 10));
					addEtatDeclarationRetournee(di, date(anneeFinale, 10, 3));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, anneeInitiale, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats de la prise en compte des données de retour
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				// remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("La déclaration 2016/1 a été transformée en 2015/43 suite au déplacement de la date de fin d'exercice commercial du 30.06.2016 au 31.12.2015 par retour de la DI.", remarque.getTexte());

				// bouclements ?
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(3, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 6, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(6, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 12, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(2);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}

				// tâches de contrôle de dossier
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(entreprise);
				criterion.setInclureTachesAnnulees(true);
				criterion.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(criterion);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				final Tache tache = tachesControle.get(0);
				Assert.assertEquals(TacheControleDossier.class, tache.getClass());
				Assert.assertEquals("Retour DI - Changement de période fiscale", tache.getCommentaire());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertFalse(tache.isAnnule());

				// et finalement les déclarations

				// aucune déclaration sur la période d'avant
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeInitiale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(0, declarations.size());
				}

				// et deux déclarations sur la période d'après
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeFinale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(2, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(dateDebutEntreprise, di.getDateDebut());
						Assert.assertEquals(dateDebutEntreprise, di.getDateDebutExerciceCommercial());
						Assert.assertEquals(date(anneeFinale, 6, 30), di.getDateFin());
						Assert.assertEquals(date(anneeFinale, 6, 30), di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 42, di.getNumero());
						Assert.assertNull(di.getModeleDocument());
					}
					{
						final DeclarationImpotOrdinairePM di = declarations.get(1);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(date(anneeFinale, 7, 1), di.getDateDebut());
						Assert.assertEquals(date(anneeFinale, 7, 1), di.getDateDebutExerciceCommercial());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFin());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 43, di.getNumero());
						Assert.assertNull(di.getModeleDocument());
					}
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialAnneeDifferenteAvantAvecDeclarationNonRetourneeUlterieure() throws Exception {

		final int anneeInitiale = 2014;
		final int anneeFinale = 2013;
		final int anneeSuivante = Math.max(anneeInitiale, anneeFinale) + 1;
		final RegDate dateDebutEntreprise = date(Math.min(anneeInitiale, anneeFinale), 2, 1);
		final RegDate ancienneFinExerciceCommercial = date(anneeInitiale, 6, 30);
		final RegDate nouvelleFinExerciceCommercial = date(anneeFinale, 12, 31);
		final RegDate dateFaillite = date(anneeSuivante, 6, 10);

		// mise en place fiscale (je ne veux pas de génération de tâche d'envoi pour les années 2016 et suivantes...)
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Echallens);

				final PeriodeFiscale pfSuivante = addPeriodeFiscale(anneeSuivante);
				final PeriodeFiscale pfInitiale = addPeriodeFiscale(anneeInitiale);
				final PeriodeFiscale pfFinale = addPeriodeFiscale(anneeFinale);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfFinale);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// déclaration retournée
				{
					final ModeleDocument mdInitiale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfInitiale);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfInitiale, date(anneeFinale, 7, 1), ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdInitiale);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, nouvelleFinExerciceCommercial.addDays(12));
				}

				// déclaration non-retournée ultérieure
				{
					final ModeleDocument mdSuivante = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfSuivante);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfSuivante, ancienneFinExerciceCommercial.getOneDayAfter(), ancienneFinExerciceCommercial.addYears(1), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdSuivante);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addYears(1).addDays(4));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, anneeInitiale, 1, infosEntreprise, null);

		// traitement de ces données
		// exceptionnellement, on va dire que les tâches d'envoi de DI sont générables depuis 2014 (pour bien voir que le trou est remplit par une tâche d'envoi de DI)
		doInNewTransactionAndSessionWithInitCleanup(new ChangementPremiereAnneeDeclarationPMInitCleanupCallback(2014), new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats de la prise en compte des données de retour
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				// remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("La déclaration 2014/1 a été transformée en 2013/1 suite au déplacement de la date de fin d'exercice commercial du 30.06.2014 au 31.12.2013 par retour de la DI.", remarque.getTexte());

				// bouclements ?
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(3, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 6, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(6, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 12, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(2);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}

				// tâches de contrôle de dossier
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(entreprise);
					criterion.setInclureTachesAnnulees(true);
					criterion.setTypeTache(TypeTache.TacheControleDossier);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheControleDossier.class, tache.getClass());
					Assert.assertEquals("Retour DI - Changement de période fiscale", tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
				}

				// tâches d'envoi de DI
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(entreprise);
					criterion.setInclureTachesAnnulees(false);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPM);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());
					Assert.assertNull(tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
					final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(anneeInitiale, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(anneeInitiale, 1, 1), tacheEnvoi.getDateDebutExercice());
					Assert.assertEquals(date(anneeInitiale, 12, 31), tacheEnvoi.getDateFin());
					Assert.assertEquals(date(anneeInitiale, 12, 31), tacheEnvoi.getDateFinExercice());
				}

				// et finalement les déclarations

				// aucune déclaration sur la période d'avant
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeInitiale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(0, declarations.size());
				}

				// une sur la période d'après
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeFinale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(1, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(date(anneeFinale, 7, 1), di.getDateDebut());
						Assert.assertEquals(date(anneeFinale, 7, 1), di.getDateDebutExerciceCommercial());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFin());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 1, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
				}

				// une sur la période suivante
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeSuivante, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(1, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(date(anneeSuivante, 1, 1), di.getDateDebut());
						Assert.assertEquals(date(anneeSuivante, 1, 1), di.getDateDebutExerciceCommercial());
						Assert.assertEquals(date(anneeSuivante, 12, 31), di.getDateFin());
						Assert.assertEquals(date(anneeSuivante, 12, 31), di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 1, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialAnneeDifferenteAvantAvecDeclarationRetourneeUlterieure() throws Exception {

		final int anneeInitiale = 2014;
		final int anneeFinale = 2013;
		final int anneeSuivante = Math.max(anneeInitiale, anneeFinale) + 1;
		final RegDate dateDebutEntreprise = date(Math.min(anneeInitiale, anneeFinale), 2, 1);
		final RegDate ancienneFinExerciceCommercial = date(anneeInitiale, 6, 30);
		final RegDate nouvelleFinExerciceCommercial = date(anneeFinale, 12, 31);
		final RegDate dateFaillite = date(anneeSuivante, 6, 10);

		// mise en place fiscale (je ne veux pas de génération de tâche d'envoi pour les années 2016 et suivantes...)
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Echallens);

				final PeriodeFiscale pfSuivante = addPeriodeFiscale(anneeSuivante);
				final PeriodeFiscale pfInitiale = addPeriodeFiscale(anneeInitiale);
				final PeriodeFiscale pfFinale = addPeriodeFiscale(anneeFinale);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfFinale);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// déclaration retournée
				{
					final ModeleDocument mdInitiale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfInitiale);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfInitiale, date(anneeFinale, 7, 1), ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdInitiale);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, nouvelleFinExerciceCommercial.addDays(12));
				}

				// déclaration retournée ultérieure
				{
					final ModeleDocument mdSuivante = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfSuivante);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfSuivante, ancienneFinExerciceCommercial.getOneDayAfter(), ancienneFinExerciceCommercial.addYears(1), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdSuivante);
					addEtatDeclarationEmise(di, dateFaillite.addDays(30));
					addEtatDeclarationRetournee(di, dateFaillite.addDays(50));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, anneeInitiale, 1, infosEntreprise, null);

		// traitement de ces données
		// exceptionnellement, on va dire que les tâches d'envoi de DI sont générables depuis 2014 (pour bien voir que le trou est remplit par une DI)
		final InitCleanupCallback initCleanup = new InitCleanupCallback() {
			private Integer oldPremiereAnnee;

			@Override
			public void init() throws Exception {
				oldPremiereAnnee = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
				parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(2014);
			}

			@Override
			public void cleanup() throws Exception {
				parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(oldPremiereAnnee);
			}
		};
		doInNewTransactionAndSessionWithInitCleanup(initCleanup, new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats de la prise en compte des données de retour
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				// remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("La déclaration 2014/1 a été transformée en 2013/1 suite au déplacement de la date de fin d'exercice commercial du 30.06.2014 au 31.12.2013 par retour de la DI.", remarque.getTexte());

				// bouclements ?
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(4, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 6, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(6, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeInitiale, 6, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
					Assert.assertEquals(6, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(2);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(dateFaillite.getLastDayOfTheMonth().getOneDayAfter().addMonths(-1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(3);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}

				// tâches de contrôle de dossier
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(entreprise);
					criterion.setInclureTachesAnnulees(true);
					criterion.setTypeTache(TypeTache.TacheControleDossier);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheControleDossier.class, tache.getClass());
					Assert.assertEquals("Retour DI - Changement de période fiscale avec déclaration retournée ultérieure", tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
				}

				// tâches d'envoi de DI
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(entreprise);
					criterion.setInclureTachesAnnulees(false);
					criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPM);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheEnvoiDeclarationImpotPM.class, tache.getClass());
					Assert.assertNull(tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
					final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
					Assert.assertEquals(date(anneeInitiale, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(anneeInitiale, 1, 1), tacheEnvoi.getDateDebutExercice());
					Assert.assertEquals(date(anneeInitiale, 12, 31), tacheEnvoi.getDateFin());
					Assert.assertEquals(date(anneeInitiale, 12, 31), tacheEnvoi.getDateFinExercice());
				}

				// et finalement les déclarations

				// aucune déclaration sur la période d'avant
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeInitiale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(0, declarations.size());
				}

				// une sur la période d'après
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeFinale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(1, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(date(anneeFinale, 7, 1), di.getDateDebut());
						Assert.assertEquals(date(anneeFinale, 7, 1), di.getDateDebutExerciceCommercial());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFin());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 1, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
				}

				// une sur la période suivante
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeSuivante, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(1, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(date(anneeSuivante, 1, 1), di.getDateDebut());
						Assert.assertEquals(date(anneeSuivante, 1, 1), di.getDateDebutExerciceCommercial());
						Assert.assertEquals(date(anneeSuivante, 6, 30), di.getDateFin());
						Assert.assertEquals(date(anneeSuivante, 6, 30), di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 1, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialAnneeDifferenteApres() throws Exception {

		final int anneeInitiale = 2015;
		final int anneeFinale = 2016;
		final RegDate dateDebutEntreprise = date(Math.min(anneeInitiale, anneeFinale), 2, 1);
		final RegDate ancienneFinExerciceCommercial = date(anneeInitiale, 12, 31);
		final RegDate nouvelleFinExerciceCommercial = date(anneeFinale, 6, 30);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pfInitiale = addPeriodeFiscale(anneeInitiale);
				final PeriodeFiscale pfFinale = addPeriodeFiscale(anneeFinale);

				final ModeleDocument mdInitiale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfInitiale);
				final ModeleDocument mdFinale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfFinale);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// déclaration retournée
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfInitiale, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdInitiale);
				addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
				addEtatDeclarationRetournee(di, nouvelleFinExerciceCommercial.addDays(12));

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, anneeInitiale, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats de la prise en compte des données de retour
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				// remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("La déclaration 2015/1 a été transformée en 2016/1 suite au déplacement de la date de fin d'exercice commercial du 31.12.2015 au 30.06.2016 par retour de la DI.", remarque.getTexte());

				// bouclements ?
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(2, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 6, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}

				// tâches de contrôle de dossier
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(entreprise);
				criterion.setInclureTachesAnnulees(true);
				criterion.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(criterion);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				final Tache tache = tachesControle.get(0);
				Assert.assertEquals(TacheControleDossier.class, tache.getClass());
				Assert.assertEquals("Retour DI - Changement de période fiscale", tache.getCommentaire());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertFalse(tache.isAnnule());

				// et finalement les déclarations

				// aucune déclaration sur la période d'avant
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeInitiale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(0, declarations.size());
				}

				// et une sur la période d'après
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeFinale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(1, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(dateDebutEntreprise, di.getDateDebut());
						Assert.assertEquals(dateDebutEntreprise, di.getDateDebutExerciceCommercial());
						Assert.assertEquals(date(anneeFinale, 6, 30), di.getDateFin());
						Assert.assertEquals(date(anneeFinale, 6, 30), di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 1, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialAnneeDifferenteApresAvecDeclarationNonRetourneeUlterieure() throws Exception {

		final int anneeInitiale = 2013;
		final int anneeFinale = 2014;
		final RegDate dateDebutEntreprise = date(Math.min(anneeInitiale, anneeFinale), 2, 1);
		final RegDate ancienneFinExerciceCommercial = date(anneeInitiale, 12, 31);
		final RegDate nouvelleFinExerciceCommercial = date(anneeFinale, 6, 30);
		final RegDate dateFaillite = date(anneeFinale, 6, 10);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Echallens);


				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// déclaration retournée
				{
					final PeriodeFiscale pfInitiale = addPeriodeFiscale(anneeInitiale);
					final ModeleDocument mdInitiale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfInitiale);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfInitiale, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdInitiale);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, nouvelleFinExerciceCommercial.addDays(12));
				}

				// déclaration présente ultérieure mais non-retournée encore
				{
					final PeriodeFiscale pfFinale = addPeriodeFiscale(anneeFinale);
					final ModeleDocument mdFinale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfFinale);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfFinale, ancienneFinExerciceCommercial.getOneDayAfter(), ancienneFinExerciceCommercial.addYears(1), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdFinale);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addYears(1).addDays(5));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, anneeInitiale, 1, infosEntreprise, null);

		// traitement de ces données (en mettant la première année de calcul des tâches de DI PM à 2014 pour comprendre ce qui se passe sur la DI 2014)
		doInNewTransactionAndSessionWithInitCleanup(new ChangementPremiereAnneeDeclarationPMInitCleanupCallback(anneeFinale), new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats de la prise en compte des données de retour
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				// remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("La déclaration 2013/1 a été transformée en 2014/2 suite au déplacement de la date de fin d'exercice commercial du 31.12.2013 au 30.06.2014 par retour de la DI.", remarque.getTexte());

				// bouclements ?
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(2, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 6, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}

				// tâches de contrôle de dossier
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(entreprise);
					criterion.setInclureTachesAnnulees(true);
					criterion.setTypeTache(TypeTache.TacheControleDossier);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheControleDossier.class, tache.getClass());
					Assert.assertEquals("Retour DI - Changement de période fiscale", tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
				}

				// tâche d'annulation de DI (et oui, la DI 2014 initialement présente doit disparaître...)
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(entreprise);
					criterion.setInclureTachesAnnulees(true);
					criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheAnnulationDeclarationImpot.class, tache.getClass());
					Assert.assertNull(tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
					final TacheAnnulationDeclarationImpot tacheAnnulation = (TacheAnnulationDeclarationImpot) tache;
					final DeclarationImpotOrdinaire declarationAAnnuler = tacheAnnulation.getDeclaration();
					Assert.assertNotNull(declarationAAnnuler);
					Assert.assertEquals((Integer) anneeFinale, declarationAAnnuler.getPeriode().getAnnee());
					Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), declarationAAnnuler.getDateDebut());
					Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), declarationAAnnuler.getDateFin());
				}

				// et finalement les déclarations

				// aucune déclaration sur la période d'avant
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeInitiale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(0, declarations.size());
				}

				// et deux sur la période d'après
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeFinale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(2, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(dateDebutEntreprise, di.getDateDebut());
						Assert.assertEquals(dateDebutEntreprise, di.getDateDebutExerciceCommercial());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFin());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 2, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
					{
						final DeclarationImpotOrdinairePM di = declarations.get(1);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), di.getDateDebut());
						Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), di.getDateDebutExerciceCommercial());
						Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), di.getDateFin());
						Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 1, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
				}
			}
		});
	}

	@Test
	public void testChangementDateFinExerciceCommercialAnneeDifferenteApresAvecDeclarationRetourneeUlterieure() throws Exception {

		final int anneeInitiale = 2013;
		final int anneeFinale = 2014;
		final RegDate dateDebutEntreprise = date(Math.min(anneeInitiale, anneeFinale), 2, 1);
		final RegDate ancienneFinExerciceCommercial = date(anneeInitiale, 12, 31);
		final RegDate nouvelleFinExerciceCommercial = date(anneeFinale, 6, 30);
		final RegDate dateFaillite = date(anneeFinale, 8, 10);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Echallens);


				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// déclaration retournée
				{
					final PeriodeFiscale pfInitiale = addPeriodeFiscale(anneeInitiale);
					final ModeleDocument mdInitiale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfInitiale);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfInitiale, dateDebutEntreprise, ancienneFinExerciceCommercial, oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdInitiale);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addDays(5));
					addEtatDeclarationRetournee(di, nouvelleFinExerciceCommercial.addDays(12));
				}

				// déclaration présente ultérieure déjà retournée
				{
					final PeriodeFiscale pfFinale = addPeriodeFiscale(anneeFinale);
					final ModeleDocument mdFinale = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfFinale);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfFinale, ancienneFinExerciceCommercial.getOneDayAfter(), ancienneFinExerciceCommercial.addYears(1), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdFinale);
					addEtatDeclarationEmise(di, ancienneFinExerciceCommercial.addYears(1).addDays(5));
					addEtatDeclarationRetournee(di, ancienneFinExerciceCommercial.addYears(1).addMonths(1));
				}

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(nouvelleFinExerciceCommercial, null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pmId, anneeInitiale, 1, infosEntreprise, null);

		// traitement de ces données (en mettant la première année de calcul des tâches de DI PM à 2014 pour comprendre ce qui se passe sur la DI 2014)
		doInNewTransactionAndSessionWithInitCleanup(new ChangementPremiereAnneeDeclarationPMInitCleanupCallback(anneeFinale), new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats de la prise en compte des données de retour
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				// remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("La déclaration 2013/1 a été transformée en 2014/2 suite au déplacement de la date de fin d'exercice commercial du 31.12.2013 au 30.06.2014 par retour de la DI.", remarque.getTexte());

				// bouclements ?
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(3, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 6, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(6, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(date(anneeFinale, 12, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(2);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}

				// tâches de contrôle de dossier
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(entreprise);
					criterion.setInclureTachesAnnulees(true);
					criterion.setTypeTache(TypeTache.TacheControleDossier);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertEquals(TacheControleDossier.class, tache.getClass());
					Assert.assertEquals("Retour DI - Changement de période fiscale avec déclaration retournée ultérieure", tache.getCommentaire());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertFalse(tache.isAnnule());
				}

				// aucune tâche d'annulation de DI (puisque la DI avait déjà été retournée, un exercice commercial 2014 - le deuxième, donc - a dû être conservé...)
				{
					final TacheCriteria criterion = new TacheCriteria();
					criterion.setContribuable(entreprise);
					criterion.setInclureTachesAnnulees(true);
					criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
					final List<Tache> taches = tacheDAO.find(criterion);
					Assert.assertNotNull(taches);
					Assert.assertEquals(0, taches.size());
				}

				// et finalement les déclarations

				// aucune déclaration sur la période d'avant
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeInitiale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(0, declarations.size());
				}

				// et deux sur la période d'après
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeFinale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(2, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(dateDebutEntreprise, di.getDateDebut());
						Assert.assertEquals(dateDebutEntreprise, di.getDateDebutExerciceCommercial());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFin());
						Assert.assertEquals(nouvelleFinExerciceCommercial, di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 2, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
					{
						final DeclarationImpotOrdinairePM di = declarations.get(1);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), di.getDateDebut());
						Assert.assertEquals(nouvelleFinExerciceCommercial.getOneDayAfter(), di.getDateDebutExerciceCommercial());
						Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), di.getDateFin());
						Assert.assertEquals(ancienneFinExerciceCommercial.addYears(1), di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 1, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
				}
			}
		});
	}

	@Test
	public void testAdresseCourrierLibreNonReconnueSansSurchargePresente() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Ma petite entreprise SARL", "Avenue de Ratatatsointsoin 24", null, null, null, null,"1003", "Lausanne");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// pas d'adresse surchargée présente
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(0, surcharges.size());

				// mais une tâche de contrôle de dossier et une remarque doivent être là...
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Adresse non-traitée", tache.getCommentaire());

				// et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("Les données d'adresse/raison sociale trouvées dans la DI 2015/1 n'ont pas pu être interprétées de manière concluante (Ma petite entreprise SARL / Avenue de Ratatatsointsoin 24 / 1003 / Lausanne).", remarque.getTexte());
			}
		});
	}

	@Test
	public void testAdresseCourrierLibreSansSurchargePresente() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Ma petite entreprise SARL", "Avenue de Beaulieu 24", null, null, null, null,"1003", "Lausanne");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// aucune tâche de contrôle de dossier ni remarque, mais une surcharge d'adresse courrier non-permanente depuis la date de quittance
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// l'adresse maintenant...
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(1, surcharges.size());
				final AdresseTiers surcharge = surcharges.get(0);
				Assert.assertNotNull(surcharge);
				Assert.assertFalse(surcharge.isAnnule());
				Assert.assertEquals(dateQuittance, surcharge.getDateDebut());
				Assert.assertNull(surcharge.getDateFin());
				Assert.assertEquals(TypeAdresseTiers.COURRIER, surcharge.getUsage());
				Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
				final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
				Assert.assertFalse(adresseSuisse.isPermanente());
				Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				Assert.assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue(), adresseSuisse.getNumeroRue());
			}
		});
	}

	@Test
	public void testContact() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final DestinataireAdresse destinataire = new DestinataireAdresse.Organisation(null, "Ma petite entreprise SARL", null, null, "Monsieur moi-même");
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(destinataire, null, null, null, "Avenue de Ratatatsointsoin", "24", null, null, null, null, MockLocalite.Lausanne1003.getNoOrdre());
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals("Monsieur moi-même", entreprise.getPersonneContact());

				// pas d'adresse surchargée présente
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(1, surcharges.size());
				final AdresseTiers surcharge = surcharges.get(0);
				Assert.assertNotNull(surcharge);
				Assert.assertFalse(surcharge.isAnnule());
				Assert.assertEquals(dateQuittance, surcharge.getDateDebut());
				Assert.assertNull(surcharge.getDateFin());
				Assert.assertEquals(TypeAdresseTiers.COURRIER, surcharge.getUsage());
				Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
				final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
				Assert.assertFalse(adresseSuisse.isPermanente());
				Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				Assert.assertNull(adresseSuisse.getNumeroRue());
				Assert.assertEquals("Avenue de Ratatatsointsoin", adresseSuisse.getRue());
				Assert.assertEquals("24", adresseSuisse.getNumeroMaison());

				// aucune tâche de contrôle de dossier ni remarque
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());
			}
		});
	}

	@Test
	public void testContactAvecAdresseStructureeNonModifiee() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final DestinataireAdresse destinataire = new DestinataireAdresse.Organisation(null, "Ma petite entreprise SARL", null, null, "Monsieur moi-même");
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.DestinataireSeulement(destinataire);
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals("Monsieur moi-même", entreprise.getPersonneContact());

				// pas d'adresse surchargée présente
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(0, surcharges.size());

				// aucune tâche de contrôle de dossier ni remarque
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());
			}
		});
	}

	@Test
	public void testAdresseCourrierLibreAvecSurchargeIdentiquePresente() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				// surcharge d'adresse représentation qui donne donc un défaut à l'adresse courrier
				final AdresseSuisse surcharge = addAdresseSuisse(entreprise, TypeAdresseTiers.REPRESENTATION, dateDebutEntreprise, null, MockRue.Lausanne.AvenueDeBeaulieu);
				surcharge.setPermanente(false);
				surcharge.setNumeroMaison("24");

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Ma petite entreprise SARL", "avenue de beaulieu 24", null, null, null,null, "1003", "Lausanne");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// aucune tâche de contrôle de dossier ni remarque, ni même une nouvelle surcharge d'adresse, car celle-ci était déjà connue
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// l'adresse maintenant... aucun changement
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(1, surcharges.size());
				{
					final AdresseTiers surcharge = surcharges.get(0);
					Assert.assertNotNull(surcharge);
					Assert.assertFalse(surcharge.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, surcharge.getDateDebut());
					Assert.assertNull(surcharge.getDateFin());
					Assert.assertEquals(TypeAdresseTiers.REPRESENTATION, surcharge.getUsage());
					Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
					final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
					Assert.assertFalse(adresseSuisse.isPermanente());
					Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue(), adresseSuisse.getNumeroRue());
				}
			}
		});
	}

	@Test
	public void testAdresseCourrierLibreAvecSurchargeIdentiqueALaCassePresPresente() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				// surcharge d'adresse représentation qui donne donc un défaut à l'adresse courrier
				final AdresseSuisse surcharge = addAdresseSuisse(entreprise, TypeAdresseTiers.REPRESENTATION, dateDebutEntreprise, null, MockLocalite.Lausanne1003.getNoOrdre(), null);
				surcharge.setPermanente(false);
				surcharge.setRue("AVEnue dE beauLiEU 24");

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Ma petite entreprise SARL", "avenue de beaulieu 24", null, null, null, null,"1003", "Lausanne");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// aucune tâche de contrôle de dossier ni remarque, ni même une nouvelle surcharge d'adresse, car celle-ci était déjà connue
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// l'adresse maintenant... aucun changement
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(1, surcharges.size());
				{
					final AdresseTiers surcharge = surcharges.get(0);
					Assert.assertNotNull(surcharge);
					Assert.assertFalse(surcharge.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, surcharge.getDateDebut());
					Assert.assertNull(surcharge.getDateFin());
					Assert.assertEquals(TypeAdresseTiers.REPRESENTATION, surcharge.getUsage());
					Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
					final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
					Assert.assertFalse(adresseSuisse.isPermanente());
					Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertNull(adresseSuisse.getNumeroRue());
					Assert.assertEquals("AVEnue dE beauLiEU 24", adresseSuisse.getRue());
				}
			}
		});
	}

	@Test
	public void testAdresseCourrierLibreAvecSurchargeNonPermanenteDifferentePresente() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				// surcharge d'adresse courrier non-permanente
				final AdresseSuisse surcharge = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Echallens.GrandRue);
				surcharge.setPermanente(false);
				surcharge.setNumeroMaison("12");

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Ma petite entreprise SARL", "Avenue de Beaulieu 24", null, null, null,null, "1003", "Lausanne");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// aucune tâche de contrôle de dossier ni remarque, mais une surcharge d'adresse courrier non-permanente depuis la date de quittance
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// l'adresse maintenant...
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(2, surcharges.size());
				{
					final AdresseTiers surcharge = surcharges.get(0);
					Assert.assertNotNull(surcharge);
					Assert.assertFalse(surcharge.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, surcharge.getDateDebut());
					Assert.assertEquals(dateQuittance.getOneDayBefore(), surcharge.getDateFin());
					Assert.assertEquals(TypeAdresseTiers.COURRIER, surcharge.getUsage());
					Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
					final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
					Assert.assertFalse(adresseSuisse.isPermanente());
					Assert.assertEquals(MockLocalite.Echallens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals(MockRue.Echallens.GrandRue.getNoRue(), adresseSuisse.getNumeroRue());
				}
				{
					final AdresseTiers surcharge = surcharges.get(1);
					Assert.assertNotNull(surcharge);
					Assert.assertFalse(surcharge.isAnnule());
					Assert.assertEquals(dateQuittance, surcharge.getDateDebut());
					Assert.assertNull(surcharge.getDateFin());
					Assert.assertEquals(TypeAdresseTiers.COURRIER, surcharge.getUsage());
					Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
					final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
					Assert.assertFalse(adresseSuisse.isPermanente());
					Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue(), adresseSuisse.getNumeroRue());
				}
			}
		});
	}

	@Test
	public void testAdresseCourrierLibreAvecSurchargeNonPermanenteDifferentePresenteMaisDejaFermee() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				// surcharge d'adresse courrier non-permanente mais fermée
				final AdresseSuisse surcharge = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, dateDebutEntreprise, dateQuittance.addMonths(1), MockRue.Echallens.GrandRue);
				surcharge.setPermanente(false);
				surcharge.setNumeroMaison("12");

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Ma petite entreprise SARL", "Avenue de Beaulieu 24", null, null, null,null, "1003", "Lausanne");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// l'adresse permanente ne doit pas avoir été touchée
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(1, surcharges.size());
				{
					final AdresseTiers surcharge = surcharges.get(0);
					Assert.assertNotNull(surcharge);
					Assert.assertFalse(surcharge.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, surcharge.getDateDebut());
					Assert.assertEquals(dateQuittance.addMonths(1), surcharge.getDateFin());
					Assert.assertEquals(TypeAdresseTiers.COURRIER, surcharge.getUsage());
					Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
					final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
					Assert.assertFalse(adresseSuisse.isPermanente());
					Assert.assertEquals(MockLocalite.Echallens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals(MockRue.Echallens.GrandRue.getNoRue(), adresseSuisse.getNumeroRue());
				}

				// une tâche de contrôle de dossier et une remarque
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Adresse non-traitée", tache.getCommentaire());

				// et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("L'adresse récupérée dans la DI 2015/1 (Ma petite entreprise SARL / Avenue de Beaulieu 24 / 1003 / Lausanne) n'a pas été prise en compte automatiquement en raison de la présence au 13.05.2016 d'une surcharge fermée d'adresse courrier.", remarque.getTexte());
			}
		});
	}

	@Test
	public void testAdresseCourrierLibreAvecSurchargePermanentePresente() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				// surcharge d'adresse courrier permanente
				final AdresseSuisse surcharge = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Echallens.GrandRue);
				surcharge.setPermanente(true);
				surcharge.setNumeroMaison("12");

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Ma petite entreprise SARL", "Avenue de Beaulieu 24", null, null, null,null, "1003", "Lausanne");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// l'adresse permanente ne doit pas avoir été touchée
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(1, surcharges.size());
				{
					final AdresseTiers surcharge = surcharges.get(0);
					Assert.assertNotNull(surcharge);
					Assert.assertFalse(surcharge.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, surcharge.getDateDebut());
					Assert.assertNull(surcharge.getDateFin());
					Assert.assertEquals(TypeAdresseTiers.COURRIER, surcharge.getUsage());
					Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
					final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
					Assert.assertTrue(adresseSuisse.isPermanente());
					Assert.assertEquals(MockLocalite.Echallens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals(MockRue.Echallens.GrandRue.getNoRue(), adresseSuisse.getNumeroRue());
				}

				// une tâche de contrôle de dossier et une remarque
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Adresse non-traitée", tache.getCommentaire());

				// et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("L'adresse récupérée dans la DI 2015/1 (Ma petite entreprise SARL / Avenue de Beaulieu 24 / 1003 / Lausanne) n'a pas été prise en compte automatiquement en raison de la présence au 13.05.2016 d'une surcharge permanente d'adresse courrier.", remarque.getTexte());
			}
		});
	}

	@Test
	public void testAnnonceChangementRaisonSociale() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				// surcharge d'adresse courrier permanente (pour vérifier que même en présence d'une adresse permanente, la raison sociale est inspectée)
				final AdresseSuisse surcharge = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Echallens.GrandRue);
				surcharge.setPermanente(true);
				surcharge.setNumeroMaison("12");

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Ma grande entreprise SARL", "Avenue de Beaulieu 24", null, null, null,null, "1003", "Lausanne");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat obtenu
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// l'adresse permanente ne doit pas avoir été touchée
				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(1, surcharges.size());
				{
					final AdresseTiers surcharge = surcharges.get(0);
					Assert.assertNotNull(surcharge);
					Assert.assertFalse(surcharge.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, surcharge.getDateDebut());
					Assert.assertNull(surcharge.getDateFin());
					Assert.assertEquals(TypeAdresseTiers.COURRIER, surcharge.getUsage());
					Assert.assertEquals(AdresseSuisse.class, surcharge.getClass());
					final AdresseSuisse adresseSuisse = (AdresseSuisse) surcharge;
					Assert.assertTrue(adresseSuisse.isPermanente());
					Assert.assertEquals(MockLocalite.Echallens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals(MockRue.Echallens.GrandRue.getNoRue(), adresseSuisse.getNumeroRue());
				}

				// deux tâches de contrôle de dossier et deux remarques (1 pour l'adresse permanente, 1 pour le changement de raison sociale)
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(2, taches.size());
				final List<Tache> tachesTriees = new ArrayList<>(taches);
				tachesTriees.sort(Comparator.comparingLong(Tache::getId));      // comme l'algorithme s'intéresse d'abord à l'adresse, puis à la raison sociale, l'ordre est ainsi connu
				{
					final Tache tache = tachesTriees.get(0);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Adresse non-traitée", tache.getCommentaire());
				}
				{
					final Tache tache = tachesTriees.get(1);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Changement de raison sociale", tache.getCommentaire());
				}

				// et les remarques ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(2, remarques.size());
				final List<Remarque> remarquesTriees = new ArrayList<>(remarques);
				remarquesTriees.sort(Comparator.comparingLong(Remarque::getId));    // comme l'algorithme s'intéresse d'abord à l'adresse, puis à la raison sociale, l'ordre est ainsi connu
				{
					final Remarque remarque = remarquesTriees.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("L'adresse récupérée dans la DI 2015/1 (Ma grande entreprise SARL / Avenue de Beaulieu 24 / 1003 / Lausanne) n'a pas été prise en compte automatiquement en raison de la présence au 13.05.2016 d'une surcharge permanente d'adresse courrier.",
					                    remarque.getTexte());
				}
				{
					final Remarque remarque = remarquesTriees.get(1);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("Nouvelle raison sociale annoncée (Ma grande entreprise SARL) dans la DI 2015/1.", remarque.getTexte());
				}
			}
		});
	}

	@Test
	public void testChangementSiegeSansChangement() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final Localisation siege = new Localisation.SaisieLibre("CossoNay");
		Assert.assertNotNull(siege.transcriptionFiscale(serviceInfra, RegDate.get()));      // vérification qu'on reconnait bien le siège à Cossonay
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, siege, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résulats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// siège inchangé
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, true);
				Assert.assertNotNull(sieges);
				Assert.assertEquals(1, sieges.size());
				final DomicileHisto siege = sieges.get(0);
				Assert.assertNotNull(siege);
				Assert.assertFalse(siege.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, siege.getDateDebut());
				Assert.assertNull(siege.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, siege.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), siege.getNumeroOfsAutoriteFiscale());

				// aucune remarque ni tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());
			}
		});
	}

	@Test
	public void testChangementSiegeCommuneValide() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final Localisation siege = new Localisation.SaisieLibre("LausannE");
		Assert.assertNotNull(siege.transcriptionFiscale(serviceInfra, RegDate.get()));      // vérification qu'on reconnait bien le siège à Cossonay
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, siege, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résulats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// siège inchangé
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, true);
				Assert.assertNotNull(sieges);
				Assert.assertEquals(1, sieges.size());
				final DomicileHisto siege = sieges.get(0);
				Assert.assertNotNull(siege);
				Assert.assertFalse(siege.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, siege.getDateDebut());
				Assert.assertNull(siege.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, siege.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), siege.getNumeroOfsAutoriteFiscale());

				// 1 remarque et 1 tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Changement de siège", tache.getCommentaire());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("Nouveau siège déclaré dans la DI 2015/1 : Lausanne (VD).", remarque.getTexte());
			}
		});
	}

	@Test
	public void testChangementSiegePaysValide() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final Localisation siege = new Localisation.Etranger(MockPays.Allemagne.getNoOFS(), "Stuttgart");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, siege, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résulats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// siège inchangé
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, true);
				Assert.assertNotNull(sieges);
				Assert.assertEquals(1, sieges.size());
				final DomicileHisto siege = sieges.get(0);
				Assert.assertNotNull(siege);
				Assert.assertFalse(siege.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, siege.getDateDebut());
				Assert.assertNull(siege.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, siege.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), siege.getNumeroOfsAutoriteFiscale());

				// 1 remarque et 1 tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Changement de siège", tache.getCommentaire());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("Nouveau siège déclaré dans la DI 2015/1 : Stuttgart (Allemagne).", remarque.getTexte());
			}
		});
	}

	@Test
	public void testChangementSiegeInconnu() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final Localisation siege = new Localisation.SaisieLibre("Pétahouchnock");
		Assert.assertNull(siege.transcriptionFiscale(serviceInfra, RegDate.get()));
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, siege, null, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résulats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// siège inchangé
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, true);
				Assert.assertNotNull(sieges);
				Assert.assertEquals(1, sieges.size());
				final DomicileHisto siege = sieges.get(0);
				Assert.assertNotNull(siege);
				Assert.assertFalse(siege.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, siege.getDateDebut());
				Assert.assertNull(siege.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, siege.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), siege.getNumeroOfsAutoriteFiscale());

				// 1 remarque et 1 tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Changement de siège", tache.getCommentaire());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("L'information de siège présente dans la DI 2015/1 (Pétahouchnock) n'a pas pu être interprétée automatiquement.", remarque.getTexte());
			}
		});
	}

	@Test
	public void testChangementAdministrationEffectiveSansChangement() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final Localisation ae = new Localisation.SaisieLibre("ÉchaLLENS");
		Assert.assertNotNull(ae.transcriptionFiscale(serviceInfra, RegDate.get()));      // vérification qu'on reconnait bien "échallens"
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, ae, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résulats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// siège inchangé
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, true);
				Assert.assertNotNull(sieges);
				Assert.assertEquals(1, sieges.size());
				final DomicileHisto siege = sieges.get(0);
				Assert.assertNotNull(siege);
				Assert.assertFalse(siege.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, siege.getDateDebut());
				Assert.assertNull(siege.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, siege.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), siege.getNumeroOfsAutoriteFiscale());

				// for principal inchangé
				final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDebutEntreprise, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());

				// aucune remarque ni tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());
			}
		});
	}

	@Test
	public void testChangementAdministrationEffectiveCommuneValide() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final Localisation ae = new Localisation.SaisieLibre("LausannE");
		Assert.assertNotNull(ae.transcriptionFiscale(serviceInfra, RegDate.get()));      // vérification qu'on reconnait bien la donnée
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, ae, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résulats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// siège inchangé
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, true);
				Assert.assertNotNull(sieges);
				Assert.assertEquals(1, sieges.size());
				final DomicileHisto siege = sieges.get(0);
				Assert.assertNotNull(siege);
				Assert.assertFalse(siege.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, siege.getDateDebut());
				Assert.assertNull(siege.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, siege.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), siege.getNumeroOfsAutoriteFiscale());

				// for principal inchangé
				final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDebutEntreprise, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());

				// 1 remarque et 1 tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Changement d'administration effective", tache.getCommentaire());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("Nouvelle administration effective déclarée dans la DI 2015/1 : Lausanne (VD).", remarque.getTexte());
			}
		});
	}

	@Test
	public void testChangementAdministrationEffectivePaysValide() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final Localisation ae = new Localisation.Etranger(MockPays.Allemagne.getNoOFS(), "Stuttgart");
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, ae, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résulats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// siège inchangé
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, true);
				Assert.assertNotNull(sieges);
				Assert.assertEquals(1, sieges.size());
				final DomicileHisto siege = sieges.get(0);
				Assert.assertNotNull(siege);
				Assert.assertFalse(siege.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, siege.getDateDebut());
				Assert.assertNull(siege.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, siege.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), siege.getNumeroOfsAutoriteFiscale());

				// for principal inchangé
				final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDebutEntreprise, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());

				// 1 remarque et 1 tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Changement d'administration effective", tache.getCommentaire());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("Nouvelle administration effective déclarée dans la DI 2015/1 : Stuttgart (Allemagne).", remarque.getTexte());
			}
		});
	}

	@Test
	public void testChangementAdministrationEffectiveInconnue() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final Localisation ae = new Localisation.SaisieLibre("Pétahouchnock");
		Assert.assertNull(ae.transcriptionFiscale(serviceInfra, RegDate.get()));
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, ae, null, null, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résulats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				// siège inchangé
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, true);
				Assert.assertNotNull(sieges);
				Assert.assertEquals(1, sieges.size());
				final DomicileHisto siege = sieges.get(0);
				Assert.assertNotNull(siege);
				Assert.assertFalse(siege.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, siege.getDateDebut());
				Assert.assertNull(siege.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, siege.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), siege.getNumeroOfsAutoriteFiscale());

				// for principal inchangé
				final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDebutEntreprise, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());

				// 1 remarque et 1 tâche de contrôle de dossier
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setNumeroCTB(idEntreprise);
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setInclureTachesAnnulees(true);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Changement d'administration effective", tache.getCommentaire());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("L'information d'administration effective présente dans la DI 2015/1 (Pétahouchnock) n'a pas pu être interprétée automatiquement.", remarque.getTexte());
			}
		});
	}

	@Test
	public void testCoordonneesFinancieresIbanValideSeul() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final String ibanConnu = null;
		final String titulaireCompteConnu = null;
		final String nouvelIban = "CH690023000123456789A";      // valide
		final String nouveauTitulaireCompte = null;

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				entreprise.setCoordonneesFinancieres(ibanConnu != null ? new CoordonneesFinancieres(ibanConnu, null) : null);
				entreprise.setTitulaireCompteBancaire(titulaireCompteConnu);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (iban valide sans titulaire)
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, null, nouvelIban, nouveauTitulaireCompte, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(nouvelIban, entreprise.getCoordonneesFinancieres().getIban());
				Assert.assertEquals("Ma petite entreprise SARL", entreprise.getTitulaireCompteBancaire());

				// remarque -> rien
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// tâche de contrôle de dossier -> aucune
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(idEntreprise);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
			}
		});
	}

	@Test
	public void testCoordonneesFinancieresIbanValideSeulRemplacantAutreIbanValide() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final String ibanConnu = "DE43123456780000087512";      // valide
		final String titulaireCompteConnu = "Otto Müllinger";   // même le titulaire du compte est écrasé
		final String nouvelIban = "CH690023000123456789A";      // valide
		final String nouveauTitulaireCompte = null;

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				entreprise.setCoordonneesFinancieres(ibanConnu != null ? new CoordonneesFinancieres(ibanConnu, null) : null);
				entreprise.setTitulaireCompteBancaire(titulaireCompteConnu);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, null, nouvelIban, nouveauTitulaireCompte, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(nouvelIban, entreprise.getCoordonneesFinancieres().getIban());
				Assert.assertEquals("Ma petite entreprise SARL", entreprise.getTitulaireCompteBancaire());

				// remarque -> rien
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// tâche de contrôle de dossier -> aucune
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(idEntreprise);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
			}
		});
	}

	@Test
	public void testCoordonneesFinancieresIbanInvalideSeul() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final String ibanConnu = null;
		final String titulaireCompteConnu = null;
		final String nouvelIban = "CH410023000123456789A";      // invalide (les chiffres de contrôle devraient être "69", pas "41")
		final String nouveauTitulaireCompte = null;

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				entreprise.setCoordonneesFinancieres(ibanConnu != null ? new CoordonneesFinancieres(ibanConnu, null) : null);
				entreprise.setTitulaireCompteBancaire(titulaireCompteConnu);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (iban valide sans titulaire)
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, null, nouvelIban, nouveauTitulaireCompte, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(nouvelIban, entreprise.getCoordonneesFinancieres().getIban());
				Assert.assertEquals("Ma petite entreprise SARL", entreprise.getTitulaireCompteBancaire());

				// remarque -> rien
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// tâche de contrôle de dossier -> aucune
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(idEntreprise);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
			}
		});
	}

	@Test
	public void testCoordonneesFinancieresIbanInvalideSeulRemplacantIbanValide() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final String ibanConnu = "DE43123456780000087512";      // valide
		final String titulaireCompteConnu = "Otto Müllmeier";   // ne devrait pas être remplacé
		final String nouvelIban = "CH410023000123456789A";      // invalide (les chiffres de contrôle devraient être "69", pas "41")
		final String nouveauTitulaireCompte = null;

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				entreprise.setCoordonneesFinancieres(ibanConnu != null ? new CoordonneesFinancieres(ibanConnu, null) : null);
				entreprise.setTitulaireCompteBancaire(titulaireCompteConnu);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, null, nouvelIban, nouveauTitulaireCompte, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(ibanConnu, entreprise.getCoordonneesFinancieres().getIban());
				Assert.assertEquals(titulaireCompteConnu, entreprise.getTitulaireCompteBancaire());

				// remarque -> 1
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals("Le numéro de compte bancaire (CH410023000123456789A) déclaré dans la DI 2015/1 est invalide, et n'a donc pas écrasé le numéro valide connu.", remarque.getTexte());

				// tâche de contrôle de dossier -> 1
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(idEntreprise);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals("Retour DI - Compte bancaire", tache.getCommentaire());
			}
		});
	}

	@Test
	public void testCoordonneesFinancieresIbanInvalideSeulRemplacantIbanInvalide() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final String ibanConnu = "DE12123456780000087512";      // invalide (les chiffres de contrôle devraient être "43", pas "12")
		final String titulaireCompteConnu = "Otto Müllmeier";   // ne devrait pas être remplacé
		final String nouvelIban = "CH410023000123456789A";      // invalide (les chiffres de contrôle devraient être "69", pas "41")
		final String nouveauTitulaireCompte = null;

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				entreprise.setCoordonneesFinancieres(ibanConnu != null ? new CoordonneesFinancieres(ibanConnu, null) : null);
				entreprise.setTitulaireCompteBancaire(titulaireCompteConnu);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, null, nouvelIban, nouveauTitulaireCompte, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(nouvelIban, entreprise.getCoordonneesFinancieres().getIban());
				Assert.assertEquals("Ma petite entreprise SARL", entreprise.getTitulaireCompteBancaire());

				// remarque -> 0
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// tâche de contrôle de dossier -> 0
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(idEntreprise);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
			}
		});
	}

	@Test
	public void testCoordonneesFinancieresNouveauTitulaireSansIban() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final String ibanConnu = "DE12123456780000087512";      // invalide (les chiffres de contrôle devraient être "43", pas "12")
		final String titulaireCompteConnu = "Otto Müllmeier";   // ne devrait pas être remplacé
		final String nouvelIban = null;
		final String nouveauTitulaireCompte = "Albert Zweisteinen";

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				entreprise.setCoordonneesFinancieres(ibanConnu != null ? new CoordonneesFinancieres(ibanConnu, null) : null);
				entreprise.setTitulaireCompteBancaire(titulaireCompteConnu);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, null, nouvelIban, nouveauTitulaireCompte, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(ibanConnu, entreprise.getCoordonneesFinancieres().getIban());
				Assert.assertEquals(titulaireCompteConnu, entreprise.getTitulaireCompteBancaire());

				// remarque -> 0
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// tâche de contrôle de dossier -> 0
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(idEntreprise);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
			}
		});
	}

	@Test
	public void testCoordonneesFinancieresNouveauTitulaireAvecIbanValide() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2015, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final String ibanConnu = "DE12123456780000087512";      // invalide (les chiffres de contrôle devraient être "43", pas "12")
		final String titulaireCompteConnu = "Otto Müllmeier";   // ne devrait pas être remplacé
		final String nouvelIban = "FR4812345678901234567890123";    // valide
		final String nouveauTitulaireCompte = "Albert Zweisteinen";

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebutEntreprise, date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				entreprise.setCoordonneesFinancieres(ibanConnu != null ? new CoordonneesFinancieres(ibanConnu, null) : null);
				entreprise.setTitulaireCompteBancaire(titulaireCompteConnu);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final InformationsEntreprise infosEntreprise = new InformationsEntreprise(null, null, null, null, nouvelIban, nouveauTitulaireCompte, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, infosEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(nouvelIban, entreprise.getCoordonneesFinancieres().getIban());
				Assert.assertEquals(nouveauTitulaireCompte, entreprise.getTitulaireCompteBancaire());

				// remarque -> 0
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				// tâche de contrôle de dossier -> 0
				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				tacheCriteria.setNumeroCTB(idEntreprise);
				final List<Tache> taches = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(taches);
				Assert.assertEquals(0, taches.size());
			}
		});
	}

	@Test
	public void testFermetureMandatGeneralLien() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2010, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, date(1950, 4, 2), null, "Mandataire à toute heure SA");
				addFormeJuridique(mandataire, date(1950, 4, 2), null, FormeJuridiqueEntreprise.SA);

				addMandatGeneral(entreprise, mandataire, dateDebutEntreprise, null, true);

				return entreprise.getNumero();
			}
		});




		// réception des données de retour (en particulier, pas de mandataire)
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, null, null);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				final Mandat mandat = mandats.get(0);
				Assert.assertNotNull(mandat);
				Assert.assertFalse(mandat.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
				Assert.assertEquals(dateTraitement.getOneDayBefore(), mandat.getDateFin());
				Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
			}
		});
	}

	@Test
	public void testFermetureMandatGeneralLienAnnulation() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2010, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final RegDate dateTraitement = RegDate.get();

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, date(1950, 4, 2), null, "Mandataire à toute heure SA");
				addFormeJuridique(mandataire, date(1950, 4, 2), null, FormeJuridiqueEntreprise.SA);

				addMandatGeneral(entreprise, mandataire, dateTraitement, null, true);        // sera annulé

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (en particulier, pas de mandataire)
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, null, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				final Mandat mandat = mandats.get(0);
				Assert.assertNotNull(mandat);
				Assert.assertTrue(mandat.isAnnule());
				Assert.assertEquals(dateTraitement, mandat.getDateDebut());
				Assert.assertNull(mandat.getDateFin());
				Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
			}
		});
	}

	@Test
	public void testFermetureMandatGeneralLienChangementDateFin() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2010, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final RegDate dateTraitement = RegDate.get();

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, date(1950, 4, 2), null, "Mandataire à toute heure SA");
				addFormeJuridique(mandataire, date(1950, 4, 2), null, FormeJuridiqueEntreprise.SA);

				final Mandat mandat = addMandatGeneral(entreprise, mandataire, dateDebutEntreprise, dateTraitement, true);
				mandat.setPersonneContact("Alfonso Bertarello");
				mandat.setNoTelephoneContact("0525551247");

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = mandataire.getNumero();
				return ids;
			}
		});

		// réception des données de retour (en particulier, pas de mandataire)
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(2, mandats.size());
				mandats.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
					Assert.assertEquals(dateTraitement.getOneDayBefore(), mandat.getDateFin());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertEquals("Alfonso Bertarello", mandat.getPersonneContact());
					Assert.assertEquals("0525551247", mandat.getNoTelephoneContact());
				}
				{
					final Mandat mandat = mandats.get(1);
					Assert.assertNotNull(mandat);
					Assert.assertTrue(mandat.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
					Assert.assertEquals(dateTraitement, mandat.getDateFin());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertEquals("Alfonso Bertarello", mandat.getPersonneContact());
					Assert.assertEquals("0525551247", mandat.getNoTelephoneContact());
				}
			}
		});
	}

	@Test
	public void testFermetureMandatGeneralAdresse() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2010, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				addAdresseMandataireSuisse(entreprise, dateDebutEntreprise, null, TypeMandat.GENERAL, "Pour vous servir SA", MockRue.Geneve.AvenueGuiseppeMotta);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (en particulier, pas de mandataire)
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, null, null);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(0, mandats.size());

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				final AdresseMandataire adresse = adresses.iterator().next();
				Assert.assertNotNull(adresse);
				Assert.assertFalse(adresse.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, adresse.getDateDebut());
				Assert.assertEquals(dateTraitement.getOneDayBefore(), adresse.getDateFin());
				Assert.assertEquals(TypeMandat.GENERAL, adresse.getTypeMandat());
			}
		});
	}

	@Test
	public void testFermetureMandatGeneralAdresseAnnulation() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2010, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final RegDate dateTraitement = RegDate.get();

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				addAdresseMandataireSuisse(entreprise, dateTraitement, null, TypeMandat.GENERAL, "Pour vous servir SA", MockRue.Geneve.AvenueGuiseppeMotta);     // sera annulée

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (en particulier, pas de mandataire)
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, null, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(0, mandats.size());

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				final AdresseMandataire adresse = adresses.iterator().next();
				Assert.assertNotNull(adresse);
				Assert.assertTrue(adresse.isAnnule());
				Assert.assertEquals(dateTraitement, adresse.getDateDebut());
				Assert.assertNull(adresse.getDateFin());
				Assert.assertEquals(TypeMandat.GENERAL, adresse.getTypeMandat());
			}
		});
	}

	@Test
	public void testFermetureMandatGeneralAdresseChangementDateFin() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2010, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);
		final RegDate dateTraitement = RegDate.get();

		final Long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Etablissement etb = addEtablissement();
				addActiviteEconomique(entreprise, etb, dateDebutEntreprise, null, true);
				addDomicileEtablissement(etb, dateDebutEntreprise, null, MockCommune.Cossonay);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, date(1950, 4, 2), null, "Mandataire à toute heure SA");
				addFormeJuridique(mandataire, date(1950, 4, 2), null, FormeJuridiqueEntreprise.SA);

				addAdresseMandataireSuisse(entreprise, dateDebutEntreprise, dateTraitement, TypeMandat.GENERAL, "Pour vous servir SA", MockRue.Geneve.AvenueGuiseppeMotta);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (en particulier, pas de mandataire)
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, null, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(0, mandats.size());

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(2, adresses.size());
				final List<AdresseMandataire> adressesTriees = new ArrayList<>(adresses);
				adressesTriees.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				{
					final AdresseMandataire adresse = adressesTriees.get(0);
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, adresse.getDateDebut());
					Assert.assertEquals(dateTraitement.getOneDayBefore(), adresse.getDateFin());
					Assert.assertEquals(TypeMandat.GENERAL, adresse.getTypeMandat());
				}
				{
					final AdresseMandataire adresse = adressesTriees.get(1);
					Assert.assertNotNull(adresse);
					Assert.assertTrue(adresse.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, adresse.getDateDebut());
					Assert.assertEquals(dateTraitement, adresse.getDateFin());
					Assert.assertEquals(TypeMandat.GENERAL, adresse.getTypeMandat());
				}
			}
		});
	}

	@Test
	public void testAjoutNouveauLienMandat() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 6, 12);
		final RegDate dateQuittance = date(annee + 1, 5, 17);
		final String ideMandataire = "CHE-1162.67650";      // mauvais format, mais on devrait s'en sortir quand-même...

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au service de la communauté SA");
				addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(futurMandataire, NumeroIDEHelper.normalize(ideMandataire));

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = futurMandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (en particulier, mandataire identifié par son numéro IDE)
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, null, null, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateTraitement, mandat.getDateDebut());
					Assert.assertNull(mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testAjoutNouveauLienMandatAvecAdresseIdentiqueSpecifieeDansRetour() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 6, 12);
		final RegDate dateQuittance = date(annee + 1, 5, 17);
		final String ideMandataire = "CHE-1162.67650";      // mauvais format, mais on devrait s'en sortir quand-même...

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au service de la communauté SA");
				addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(futurMandataire, NumeroIDEHelper.normalize(ideMandataire));

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = futurMandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (ici, la particularité est qu'une adresse sans raison sociale est fournie - on notera de plus que l'adresse n'est pas
		// tout-à-fait la même, puisque le nom officiel contient un tiret...)
		final AdresseRaisonSociale adresseMandataire = new AdresseRaisonSociale.Brutte("Avenue Guiseppe Motta 42", null, null, null, null,null, MockLocalite.Geneve.getNPA().toString(), MockLocalite.Geneve.getNom());
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, adresseMandataire, null, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateTraitement, mandat.getDateDebut());
					Assert.assertNull(mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testAjoutNouveauMandataireIdentifieAvecAdresseDifferenteSpecifieeDansRetour() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 6, 12);
		final RegDate dateQuittance = date(annee + 1, 5, 17);
		final String ideMandataire = "CHE-1162.67650";      // mauvais format, mais on devrait s'en sortir quand-même...

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(futurMandataire, NumeroIDEHelper.normalize(ideMandataire));

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = futurMandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (ici, la particularité est qu'une complètement différente est fournie)
		final AdresseRaisonSociale adresseMandataire = new AdresseRaisonSociale.Brutte("Voltastrasse 42", null, null, null, null, null,MockLocalite.Zurich8044.getNPA().toString(), MockLocalite.Zurich8044.getNom());
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, adresseMandataire, null, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(0, mandats.size());

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				{
					final AdresseMandataire adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertTrue(adresse.isWithCopy());
					Assert.assertEquals(dateTraitement, adresse.getDateDebut());
					Assert.assertNull(adresse.getDateFin());
					Assert.assertEquals(TypeMandat.GENERAL, adresse.getTypeMandat());
					Assert.assertNull(adresse.getCivilite());
					Assert.assertEquals("Au service de la 'hips communauté SA", adresse.getNomDestinataire());
					Assert.assertNull(adresse.getComplement());
					Assert.assertEquals("42", adresse.getNumeroMaison());
					Assert.assertEquals(AdresseMandataireSuisse.class, adresse.getClass());
					final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresse;
					Assert.assertEquals(MockRue.Zurich.VoltaStrasse.getNoRue(), adresseSuisse.getNumeroRue());
					Assert.assertEquals(MockLocalite.Zurich8044.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				}

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testAjoutNouveauMandataireIdentifieAvecAdresseEtRaisonSocialeDifferentesSpecifieesDansRetour() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 6, 12);
		final RegDate dateQuittance = date(annee + 1, 5, 17);
		final String ideMandataire = "CHE-1162.67650";      // mauvais format, mais on devrait s'en sortir quand-même...

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(futurMandataire, NumeroIDEHelper.normalize(ideMandataire));

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = futurMandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (ici, la particularité est qu'une complètement différente est fournie)
		final AdresseRaisonSociale adresseMandataire = new AdresseRaisonSociale.Brutte("Freundlicherweise AG", "Voltastrasse 42", null, null, null,null, MockLocalite.Zurich8044.getNPA().toString(), MockLocalite.Zurich8044.getNom());
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, adresseMandataire, null, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(0, mandats.size());

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				{
					final AdresseMandataire adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertTrue(adresse.isWithCopy());
					Assert.assertEquals(dateTraitement, adresse.getDateDebut());
					Assert.assertNull(adresse.getDateFin());
					Assert.assertEquals(TypeMandat.GENERAL, adresse.getTypeMandat());
					Assert.assertNull(adresse.getCivilite());
					Assert.assertEquals("Au service de la 'hips communauté SA", adresse.getNomDestinataire());
					Assert.assertEquals("Freundlicherweise AG", adresse.getComplement());
					Assert.assertEquals("42", adresse.getNumeroMaison());
					Assert.assertEquals(AdresseMandataireSuisse.class, adresse.getClass());
					final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresse;
					Assert.assertEquals(MockRue.Zurich.VoltaStrasse.getNoRue(), adresseSuisse.getNumeroRue());
					Assert.assertEquals(MockLocalite.Zurich8044.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				}

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testAjoutNouveauMandataireIdentifieAvecAdresseNonIdentifiee() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 6, 12);
		final RegDate dateQuittance = date(annee + 1, 5, 17);
		final String ideMandataire = "CHE-1162.67650";      // mauvais format, mais on devrait s'en sortir quand-même...

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(futurMandataire, NumeroIDEHelper.normalize(ideMandataire));

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = futurMandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (ici, la particularité est que l'adresse fournie n'est pas connue dans l'infrastructure)
		final AdresseRaisonSociale adresseMandataire = new AdresseRaisonSociale.Brutte("Mandataire bidon", "Rue de la bonne arnaque 63e", null, null, null,null, MockLocalite.Renens.getNPA().toString(), MockLocalite.Renens.getNom());
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, adresseMandataire, null, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateTraitement, mandat.getDateDebut());
					Assert.assertNull(mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				{
					final Remarque remarque = remarques.iterator().next();
					Assert.assertNotNull(remarque);
					Assert.assertFalse(remarque.isAnnule());
					Assert.assertEquals("Les données d'adresse/raison sociale trouvées pour le mandataire dans la DI " + annee + "/1 n'ont pas pu être interprétées de manière concluante (Mandataire bidon / Rue de la bonne arnaque 63e / 1020 / Renens VD).", remarque.getTexte());
				}

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Mandataire", tache.getCommentaire());
				}
			}
		});
	}

	@Test
	public void testAjoutNouveauMandataireNonIdentifieAvecAdresseNonIdentifiee() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 6, 12);
		final RegDate dateQuittance = date(annee + 1, 5, 17);
		final String ideMandataire = "CHE-1162.67650";      // mauvais format, mais on devrait s'en sortir quand-même...

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(futurMandataire, NumeroIDEHelper.normalize(ideMandataire));

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = futurMandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (ici, la particularité est que l'adresse fournie n'est pas connue dans l'infrastructure)
		final AdresseRaisonSociale adresseMandataire = new AdresseRaisonSociale.Brutte("Mandataire bidon", "Rue de la bonne arnaque 63e", null, null, null,null, MockLocalite.Renens.getNPA().toString(), MockLocalite.Renens.getNom());
		final InformationsMandataire infosMandataire = new InformationsMandataire(null, adresseMandataire, null, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(0, mandats.size());

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				{
					final Remarque remarque = remarques.iterator().next();
					Assert.assertNotNull(remarque);
					Assert.assertFalse(remarque.isAnnule());
					Assert.assertEquals("Les données d'adresse/raison sociale trouvées pour le mandataire dans la DI " + annee + "/1 n'ont pas pu être interprétées de manière concluante :\n- adresse : Mandataire bidon / Rue de la bonne arnaque 63e / 1020 / Renens VD.", remarque.getTexte());
				}

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Mandataire", tache.getCommentaire());
				}
			}
		});
	}

	@Test
	public void testAjoutNouveauMandataireNonIdentifieCarResultatsMultiples() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 6, 12);
		final RegDate dateQuittance = date(annee + 1, 5, 17);
		final String ideMandataire = "CHE-1162.67650";      // mauvais format, mais on devrait s'en sortir quand-même...

		final class Ids {
			long idEntreprise;
			long idMandataire1;
			long idMandataire2;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				{
					final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
					addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
					addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
					final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
					adresse.setNumeroMaison("42");
					addIdentificationEntreprise(futurMandataire, NumeroIDEHelper.normalize(ideMandataire));     // même IDE dans les deux cas de mandataire
					ids.idMandataire1 = futurMandataire.getNumero();
				}
				{
					final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
					addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au la bonne vôtre SARL");
					addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
					final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Chur.Grabenstrasse);        // servira de défaut pour l'adresse de représentation
					adresse.setNumeroMaison("24");
					addIdentificationEntreprise(futurMandataire, NumeroIDEHelper.normalize(ideMandataire));     // même IDE dans les deux cas de mandataire
					ids.idMandataire2 = futurMandataire.getNumero();
				}

				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (ici, la particularité est que le numéro IDE est associé à deux entreprises distinctes)
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, null, null, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(0, mandats.size());

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				{
					final Remarque remarque = remarques.iterator().next();
					Assert.assertNotNull(remarque);
					Assert.assertFalse(remarque.isAnnule());
					Assert.assertEquals(String.format("Identification du mandataire pointé par le numéro IDE %s dans la DI %d/1 imprécise (2 tiers trouvés : %s, %s).",
					                                  FormatNumeroHelper.formatNumIDE(ideMandataire),
					                                  annee,
					                                  FormatNumeroHelper.numeroCTBToDisplay(ids.idMandataire1),
					                                  FormatNumeroHelper.numeroCTBToDisplay(ids.idMandataire2)),
					                    remarque.getTexte());
				}

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Mandataire", tache.getCommentaire());
				}
			}
		});
	}

	@Test
	public void testChangementMandataireLienVersAdresse() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 7, 13);
		final RegDate dateQuittance = date(annee + 1, 5, 18);
		final String ideMandataire = "CHE429111243";        // inconnu...

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(mandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(mandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");

				addMandatGeneral(entreprise, mandataire, dateDebutEntreprise, null, true);

				return entreprise.getNumero();
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (nouveau mandataire)
		final AdresseRaisonSociale adresseMandataire = new AdresseRaisonSociale.Brutte("Freundlicherweise AG", "Voltastrasse 42", null, null, null,null, MockLocalite.Zurich8044.getNPA().toString(), MockLocalite.Zurich8044.getNom());
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, adresseMandataire, Boolean.TRUE, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
					Assert.assertEquals(dateTraitement.getOneDayBefore(), mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				{
					final AdresseMandataire adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertFalse(adresse.isWithCopy());
					Assert.assertEquals(dateTraitement, adresse.getDateDebut());
					Assert.assertNull(adresse.getDateFin());
					Assert.assertEquals(TypeMandat.GENERAL, adresse.getTypeMandat());
					Assert.assertNull(adresse.getCivilite());
					Assert.assertEquals("Freundlicherweise AG", adresse.getNomDestinataire());
					Assert.assertNull(adresse.getComplement());
					Assert.assertEquals("42", adresse.getNumeroMaison());
					Assert.assertEquals(AdresseMandataireSuisse.class, adresse.getClass());
					final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresse;
					Assert.assertEquals(MockRue.Zurich.VoltaStrasse.getNoRue(), adresseSuisse.getNumeroRue());
					Assert.assertEquals(MockLocalite.Zurich8044.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				}

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testChangementMandataireAdresseVersLien() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 7, 13);
		final RegDate dateQuittance = date(annee + 1, 5, 18);
		final String ideMandataire = "CHE429111243";

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise futurMandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(futurMandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(futurMandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(futurMandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(futurMandataire, ideMandataire);

				final AdresseMandataireSuisse adresseMandataire = addAdresseMandataireSuisse(entreprise, dateDebutEntreprise, null, TypeMandat.GENERAL, "Chapi chapo", MockRue.Lausanne.AvenueGabrielDeRumine);
				adresseMandataire.setWithCopy(false);
				adresseMandataire.setNumeroMaison("17");

				return entreprise.getNumero();
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (nouveau mandataire)
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, null, Boolean.FALSE, null);
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateTraitement, mandat.getDateDebut());
					Assert.assertNull(mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				{
					final AdresseMandataire adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertFalse(adresse.isWithCopy());
					Assert.assertEquals(dateDebutEntreprise, adresse.getDateDebut());
					Assert.assertEquals(dateTraitement.getOneDayBefore(), adresse.getDateFin());
					Assert.assertEquals(TypeMandat.GENERAL, adresse.getTypeMandat());
					Assert.assertNull(adresse.getCivilite());
					Assert.assertEquals("Chapi chapo", adresse.getNomDestinataire());
					Assert.assertNull(adresse.getComplement());
					Assert.assertEquals("17", adresse.getNumeroMaison());
					Assert.assertEquals(AdresseMandataireSuisse.class, adresse.getClass());
					final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresse;
					Assert.assertEquals(MockRue.Lausanne.AvenueGabrielDeRumine.getNoRue(), adresseSuisse.getNumeroRue());
					Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				}

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testChangementFlagCopieMandataireSurLien() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 7, 13);
		final RegDate dateQuittance = date(annee + 1, 5, 18);
		final String ideMandataire = "CHE429111243";

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(mandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(mandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(mandataire, ideMandataire);

				addMandatGeneral(entreprise, mandataire, dateDebutEntreprise, null, true);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = mandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (même mandataire mais le flag "sans copie" est coché)
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, null, Boolean.TRUE, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(2, mandats.size());
				mandats.sort(new DateRangeComparator<>());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
					Assert.assertEquals(dateTraitement.getOneDayBefore(), mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}
				{
					final Mandat mandat = mandats.get(1);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateTraitement, mandat.getDateDebut());
					Assert.assertNull(mandat.getDateFin());
					Assert.assertFalse(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testChangementNoTelContactMandataireSurLien() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 7, 13);
		final RegDate dateQuittance = date(annee + 1, 5, 18);
		final String ideMandataire = "CHE429111243";
		final String oldTelContact = "0219876543";
		final String newTelContact = "0211234567";

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(mandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(mandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(mandataire, ideMandataire);

				final Mandat mandat = addMandatGeneral(entreprise, mandataire, dateDebutEntreprise, null, true);
				mandat.setNoTelephoneContact(oldTelContact);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = mandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (même mandataire mais le numéro de téléphone de contact a changé)
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, null, Boolean.FALSE, newTelContact);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(2, mandats.size());
				mandats.sort(new DateRangeComparator<>());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
					Assert.assertEquals(dateTraitement.getOneDayBefore(), mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertEquals(oldTelContact, mandat.getNoTelephoneContact());
				}
				{
					final Mandat mandat = mandats.get(1);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateTraitement, mandat.getDateDebut());
					Assert.assertNull(mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertEquals(newTelContact, mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testIndicationMandataireIdentiqueAExistant() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 7, 13);
		final RegDate dateQuittance = date(annee + 1, 5, 18);
		final String ideMandataire = "CHE429111243";

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(mandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(mandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(mandataire, ideMandataire);

				addMandatGeneral(entreprise, mandataire, dateDebutEntreprise, null, true);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = mandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (même mandataire, même flag)
		final InformationsMandataire infosMandataire = new InformationsMandataire(ideMandataire, null, Boolean.FALSE, null);
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				mandats.sort(new DateRangeComparator<>());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
					Assert.assertNull(mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	@Test
	public void testMandatairePreexistantAvecNouvelleDonneeSansIdeNiAdresseConcluante() throws Exception {

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);
		final String ideMandataire = "CHE429111243";

		final class Ids {
			long idEntreprise;
			long idMandataire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, dateDebutEntreprise, null, "Au service de la 'hips communauté SA");
				addFormeJuridique(mandataire, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				final AdresseSuisse adresse = addAdresseSuisse(mandataire, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Geneve.AvenueGuiseppeMotta);        // servira de défaut pour l'adresse de représentation
				adresse.setNumeroMaison("42");
				addIdentificationEntreprise(mandataire, ideMandataire);

				addMandatGeneral(entreprise, mandataire, dateDebutEntreprise, null, true);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idMandataire = mandataire.getNumero();
				return ids;
			}
		});

		// sync pour s'assurer que les nouveaux tiers sont bien indexés avant de continuer
		globalTiersIndexer.sync();

		// réception des données de retour (pas de numéro IDE, adresse non-reconnue)
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.Brutte("Chapi chapo", "Tralalo", "Chapo chapi", "Tralali", null,null, MockLocalite.Bussigny.getNPA().toString(), MockLocalite.Bussigny.getNom());
		final InformationsMandataire infosMandataire = new InformationsMandataire(null, adresse, Boolean.FALSE, "0211234567");
		final RetourDI retour = new RetourDI(ids.idEntreprise, annee, 1, null, infosMandataire);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				mandats.sort(new DateRangeComparator<>());
				{
					final Mandat mandat = mandats.get(0);
					Assert.assertNotNull(mandat);
					Assert.assertFalse(mandat.isAnnule());
					Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
					Assert.assertNull(mandat.getDateFin());
					Assert.assertTrue(mandat.getWithCopy());
					Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
					Assert.assertEquals((Long) ids.idMandataire, mandat.getObjetId());
					Assert.assertNull(mandat.getPersonneContact());
					Assert.assertNull(mandat.getNoTelephoneContact());
				}

				final Set<AdresseMandataire> adresses = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				{
					final Remarque remarque = remarques.iterator().next();
					Assert.assertNotNull(remarque);
					Assert.assertFalse(remarque.isAnnule());
					Assert.assertEquals("Les données d'adresse/raison sociale trouvées pour le mandataire dans la DI " + annee + "/1 n'ont pas pu être interprétées de manière concluante :\n- adresse : Chapi chapo / Tralalo / Chapo chapi / Tralali / 1030 / Bussigny\n- copie mandataire : avec\n- téléphone contact : 0211234567.", remarque.getTexte());
				}

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Mandataire", tache.getCommentaire());
				}
			}
		});
	}

	@Test
	public void testAdresseStructureeEntrepriseAvecContactMaisFlagAdresseModifieeFalse() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (pas de numéro IDE, adresse non-reconnue)
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(new DestinataireAdresse.Organisation(null, null, null, null, "M. Alfred Proutprout"), null, null, null, null, null, null, null, null, null, null);
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(id, annee, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals("M. Alfred Proutprout", entreprise.getPersonneContact());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	/**
	 * [SIFISC-21738] Effacement du numéro de téléphone utilisé pour le contact en cas de changement dans les données du contact
	 */
	@Test
	public void testEffacementNumeroTelephoneContact() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				entreprise.setPersonneContact("Le Chef");
				entreprise.setNumeroTelephonePortable("0789999999");
				entreprise.setNumeroTelephonePrive("0219999999");
				entreprise.setNumeroTelephoneProfessionnel("0213169999");
				entreprise.setNumeroTelecopie("0213160000");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(new DestinataireAdresse.Organisation(null, null, null, null, "M. Alfred Proutprout"), null, null, null, null, null, null, null, null, null, null);
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(id, annee, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals("M. Alfred Proutprout", entreprise.getPersonneContact());
				Assert.assertNull(entreprise.getNumeroTelephonePortable());
				Assert.assertNull(entreprise.getNumeroTelephonePrive());
				Assert.assertNull(entreprise.getNumeroTelephoneProfessionnel());
				Assert.assertNull(entreprise.getNumeroTelecopie());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	/**
	 * [SIFISC-22248] Le nom du contact doit être comparé de manière non-sensible à la casse
	 */
	@Test
	public void testSensibiliteCassePersonneContact() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				entreprise.setPersonneContact("Le Chef");
				entreprise.setNumeroTelephonePortable("0789999999");
				entreprise.setNumeroTelephonePrive("0219999999");
				entreprise.setNumeroTelephoneProfessionnel("0213169999");
				entreprise.setNumeroTelecopie("0213160000");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (on notera l'absence de majuscules dans "le chef")
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(new DestinataireAdresse.Organisation(null, null, null, null, "le chef"), null, null, null, null, null, null, null, null, null, null);
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, "0213168888");
		final RetourDI retour = new RetourDI(id, annee, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				// le nom a été repris, mais comme il est très semblable au précédent, on n'a pas effacé les numéros de téléphone
				Assert.assertEquals("le chef", entreprise.getPersonneContact());
				Assert.assertEquals("0789999999", entreprise.getNumeroTelephonePortable());
				Assert.assertEquals("0219999999", entreprise.getNumeroTelephonePrive());
				Assert.assertEquals("0213168888", entreprise.getNumeroTelephoneProfessionnel());
				Assert.assertEquals("0213160000", entreprise.getNumeroTelecopie());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	/**
	 * [SIFISC-21693] Ce test vérifie que le numéro de téléphone professionnel est bien copié des données de retour de la DI dans tous les cas.
	 */
	@Test
	public void testCopieNumeroTelephoneProfessionel() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);

		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
			addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

			entreprise.setPersonneContact("Le Chef");
			entreprise.setNumeroTelephonePortable("0789999999");
			entreprise.setNumeroTelephonePrive("0219999999");
			entreprise.setNumeroTelephoneProfessionnel("0213169999");
			entreprise.setNumeroTelecopie("0213160000");

			final PeriodeFiscale pf = addPeriodeFiscale(annee);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			addEtatDeclarationEmise(di, date(annee, 7, 5));
			addEtatDeclarationRetournee(di, dateQuittance);

			return entreprise.getNumero();
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(new DestinataireAdresse.Organisation(null, null, null, null, "M. Alfred Proutprout"), null, null, null, null, null, null, null, null, null, null);
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, "0666666666");
		final RetourDI retour = new RetourDI(id, annee, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals("M. Alfred Proutprout", entreprise.getPersonneContact());
				Assert.assertNull(entreprise.getNumeroTelephonePortable());
				Assert.assertNull(entreprise.getNumeroTelephonePrive());
				Assert.assertEquals("0666666666", entreprise.getNumeroTelephoneProfessionnel());
				Assert.assertNull(entreprise.getNumeroTelecopie());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	/**
	 * [SIFISC-21738] Effacement du numéro de téléphone utilisé pour le contact en cas de changement dans les données du contact
	 */
	@Test
	public void testEffacementNumeroTelephoneContactAjoutTitre() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				entreprise.setPersonneContact("Le Chef");
				entreprise.setNumeroTelephonePortable("0789999999");
				entreprise.setNumeroTelephonePrive("0219999999");
				entreprise.setNumeroTelephoneProfessionnel("0213169999");
				entreprise.setNumeroTelecopie("0213160000");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour (juste ajout d'un titre sur le nom déjà connu)
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(new DestinataireAdresse.Organisation(null, null, null, null, "M. Le Chef"), null, null, null, null, null, null, null, null, null, null);
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(id, annee, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals("M. Le Chef", entreprise.getPersonneContact());
				Assert.assertNull(entreprise.getNumeroTelephonePortable());
				Assert.assertNull(entreprise.getNumeroTelephonePrive());
				Assert.assertNull(entreprise.getNumeroTelephoneProfessionnel());
				Assert.assertNull(entreprise.getNumeroTelecopie());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	/**
	 * [SIFISC-21738] Non-effacement du numéro de téléphone utilisé pour le contact en cas de non-changement dans les données du contact
	 * [SIFISC-22366] Update: le téléphone professionnel est repris de la quittance dans tous les cas.
	 */
	@Test
	public void testNonEffacementNumeroTelephoneContactSiPasChangementContact() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				entreprise.setPersonneContact("Le Chef");
				entreprise.setNumeroTelephonePortable("0789999999");
				entreprise.setNumeroTelephonePrive("0219999999");
				entreprise.setNumeroTelephoneProfessionnel("0213169999");
				entreprise.setNumeroTelecopie("0213160000");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(new DestinataireAdresse.Organisation(null, null, null, null, "Le Chef"), null, null, null, null, null, null, null, null, null, null);
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(id, annee, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals("Le Chef", entreprise.getPersonneContact());
				Assert.assertEquals("0789999999", entreprise.getNumeroTelephonePortable());
				Assert.assertEquals("0219999999", entreprise.getNumeroTelephonePrive());
				Assert.assertNull(entreprise.getNumeroTelephoneProfessionnel());
				Assert.assertEquals("0213160000", entreprise.getNumeroTelecopie());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	/**
	 * [SIFISC-21738] Non-effacement du numéro de téléphone utilisé pour le contact en d'absence de données de contact
	 * [SIFISC-22366] Update: le téléphone professionnel est repris de la quittance dans tous les cas.
	 */
	@Test
	public void testNonEffacementNumeroTelephoneContactAbsenceDonneesContact() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				entreprise.setPersonneContact("Le Chef");
				entreprise.setNumeroTelephonePortable("0789999999");
				entreprise.setNumeroTelephonePrive("0219999999");
				entreprise.setNumeroTelephoneProfessionnel("0213169999");
				entreprise.setNumeroTelecopie("0213160000");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(new DestinataireAdresse.Organisation(null, null, null, null, null), null, null, null, null, null, null, null, null, null, null);
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(null, adresse, null, null, null, null, null);
		final RetourDI retour = new RetourDI(id, annee, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals("Le Chef", entreprise.getPersonneContact());
				Assert.assertEquals("0789999999", entreprise.getNumeroTelephonePortable());
				Assert.assertEquals("0219999999", entreprise.getNumeroTelephonePrive());
				Assert.assertNull(entreprise.getNumeroTelephoneProfessionnel());
				Assert.assertEquals("0213160000", entreprise.getNumeroTelecopie());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	/**
	 * [SIFISC-22031] Prise en compte du prénom/nom du contact mandataire, oublié jusque là...
	 */
	@Test
	public void testNomPrenomContactMandataire() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2009, 8, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 18);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite entreprise SARL");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(6, 30), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee - 1, 7, 1), date(annee, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee, 7, 5));
				addEtatDeclarationRetournee(di, dateQuittance);

				return entreprise.getNumero();
			}
		});

		// réception des données de retour
		final DestinataireAdresse destinataire = new DestinataireAdresse.Organisation(null, "Mon Mande-à-Terre", null, null, "Madame Delphine Rapon");
		final AdresseRaisonSociale adresse = new AdresseRaisonSociale.StructureeSuisse(destinataire, null, null, MockRue.Lausanne.CheminDeMornex.getNoRue(), null, "25 bis", null, null, MockLocalite.Lausanne1003.getNPA(), null, MockLocalite.Lausanne1003.getNoOrdre());
		final InformationsMandataire infoMandataire = new InformationsMandataire(null, adresse, Boolean.FALSE, "0213161111");
		final RetourDI retour = new RetourDI(id, annee, 1, null, infoMandataire);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final Set<AdresseMandataire> adressesMandataires = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adressesMandataires);
				Assert.assertEquals(1, adressesMandataires.size());
				final AdresseMandataire adresseMandataire = adressesMandataires.iterator().next();
				Assert.assertNotNull(adresseMandataire);
				Assert.assertEquals(AdresseMandataireSuisse.class, adresseMandataire.getClass());
				final AdresseMandataireSuisse adresseMandataireSuisse = (AdresseMandataireSuisse) adresseMandataire;
				Assert.assertEquals(MockRue.Lausanne.CheminDeMornex.getNoRue(), adresseMandataireSuisse.getNumeroRue());
				Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresseMandataireSuisse.getNumeroOrdrePoste());
				Assert.assertNull(adresseMandataire.getCivilite());
				Assert.assertEquals("Mon Mande-à-Terre", adresseMandataire.getNomDestinataire());
				Assert.assertEquals("0213161111", adresseMandataire.getNoTelephoneContact());
				Assert.assertEquals("Madame Delphine Rapon", adresseMandataire.getPersonneContact());

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());
			}
		});
	}

	/**
	 * SIFISC-22254 : un exercice commercial 1.7.2015 -> 30.06.2016 est tiré par la DI jusqu'en 2017
	 * (cela devrait poser un problème car alors 2016 n'a pas de bouclement alors qu'il ne s'agit pas de la première année)
	 */
	@Test
	public void testTraitementDecalageBouclementTropLoin() throws Exception {

		final int anneeInitiale = RegDate.get().year() - 1;
		final RegDate dateDebut = RegDate.get(anneeInitiale - 1, 1, 1);

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addRaisonSociale(entreprise, dateDebut, null, "Entreprise Dugenou");
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebut, DayMonth.get(6, 30), 12);      // initialement, bouclements tous les ans au 30.06
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bussigny);

				final PeriodeFiscale pfCourante = addPeriodeFiscale(anneeInitiale);
				final PeriodeFiscale pfSuivante = addPeriodeFiscale(anneeInitiale + 1);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfCourante);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfSuivante);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfCourante, date(dateDebut.year(), 7, 1), date(anneeInitiale, 6, 30), date(dateDebut.year(), 7, 1), date(anneeInitiale, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(anneeInitiale, 7, 6));
				addEtatDeclarationRetournee(di, date(anneeInitiale, 11, 1));

				return entreprise.getNumero();
			}
		});

		// arrivée du contenu de la DI avec un changement de PF -> année suivante (l'ancienne PF de la DI se retrouve alors sans bouclement du tout...)
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(date(anneeInitiale + 1, 6, 30), null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pm, anneeInitiale, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		// - la DI n'a pas changé de PF ni de dates
		// - les bouclements n'ont pas été modifiés
		// - une tâche de contrôle de dossier a été générée
		// - une remarque a été ajoutée
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals(String.format("Le retour de la DI %d/%d annonce une nouvelle fin d'exercice commercial au %s, mais l'année civile %d se retrouve alors sans bouclement, ce qui est interdit.",
				                                  anneeInitiale,
				                                  1,
				                                  RegDateHelper.dateToDisplayString(date(anneeInitiale + 1, 6, 30)),
				                                  anneeInitiale),
				                    remarque.getTexte());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Date de fin de l'exercice commercial", tache.getCommentaire());
				}

				final List<DeclarationImpotOrdinairePM> dis = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true);
				Assert.assertNotNull(dis);
				Assert.assertEquals(1, dis.size());
				final DeclarationImpotOrdinairePM di = dis.get(0);
				Assert.assertNotNull(di);
				Assert.assertFalse(di.isAnnule());
				Assert.assertEquals(date(dateDebut.year(), 7, 1), di.getDateDebut());
				Assert.assertEquals(date(dateDebut.year(), 7, 1), di.getDateDebutExerciceCommercial());
				Assert.assertEquals(date(anneeInitiale, 6, 30), di.getDateFin());
				Assert.assertEquals(date(anneeInitiale, 6, 30), di.getDateFinExerciceCommercial());

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);

				// je ne compare que les 3 premiers exercices, car un 4ème apparaît parfois dans le calcul (quand la date du jour est en fin d'année, i.e. après le 1.7 de l'année)
				Assert.assertTrue(String.valueOf(exercices.size()), exercices.size() == 3 || exercices.size() == 4);
				Assert.assertEquals(new ExerciceCommercial(dateDebut, date(dateDebut.year(), 6, 30)), exercices.get(0));
				Assert.assertEquals(new ExerciceCommercial(date(dateDebut.year(), 7, 1), date(anneeInitiale, 6, 30)), exercices.get(1));
				Assert.assertEquals(new ExerciceCommercial(date(anneeInitiale, 7, 1), date(anneeInitiale + 1, 6, 30)), exercices.get(2));
			}
		});

	}

	/**
	 * SIFISC-22254 : un exercice commercial 1.7.2015 -> 30.06.2016 est tiré par la DI jusqu'en 2017
	 * (cela devrait poser un problème car alors 2016 n'a pas de bouclement alors qu'il ne s'agit pas de la première année)
	 * --> cas où c'est la première DI qui est décalée, mais comme c'est la première, ce n'est pas un souci
	 */
	@Test
	public void testTraitementDecalagePremierBouclementAnneeSuivante() throws Exception {

		final int anneeInitiale = RegDate.get().year() - 2;
		final RegDate dateDebut = RegDate.get(anneeInitiale, 1, 1);

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addRaisonSociale(entreprise, dateDebut, null, "Entreprise Dugenou");
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebut, DayMonth.get(6, 30), 12);      // initialement, bouclements tous les ans au 30.06
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bussigny);

				final PeriodeFiscale pfCourante = addPeriodeFiscale(anneeInitiale);
				final PeriodeFiscale pfSuivante = addPeriodeFiscale(anneeInitiale + 1);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfCourante);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfSuivante);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfCourante, date(anneeInitiale, 1, 1), date(anneeInitiale, 6, 30), date(anneeInitiale, 1, 1), date(anneeInitiale, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(anneeInitiale, 7, 6));
				addEtatDeclarationRetournee(di, date(anneeInitiale + 1, 1, 20));

				return entreprise.getNumero();
			}
		});

		// arrivée du contenu de la DI avec un changement de PF -> année suivante (l'ancienne PF de la DI se retrouve alors sans bouclement du tout, mais ce n'est pas grave, car c'est la première...)
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(date(anneeInitiale + 1, 1, 31), null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pm, anneeInitiale, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat : tout s'est bien passé
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				// remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertEquals(String.format("La déclaration %d/1 a été transformée en %d/1 suite au déplacement de la date de fin d'exercice commercial du 30.06.%d au 31.01.%d par retour de la DI.",
				                                  anneeInitiale,
				                                  anneeInitiale + 1,
				                                  anneeInitiale,
				                                  anneeInitiale + 1),
				                    remarque.getTexte());

				// bouclements ?
				final List<Bouclement> bouclements = new ArrayList<>(entreprise.getBouclements());
				bouclements.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(Bouclement::getDateDebut, NullDateBehavior.EARLIEST::compare)));
				Assert.assertEquals(2, bouclements.size());
				{
					final Bouclement bouclement = bouclements.get(0);
					Assert.assertNotNull(bouclement);
					Assert.assertFalse(bouclement.isAnnule());
					Assert.assertEquals(RegDate.get(anneeInitiale + 1, 1, 1), bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(1, 31), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}
				{
					final Bouclement bouclement = bouclements.get(1);
					Assert.assertNotNull(bouclement);
					Assert.assertTrue(bouclement.isAnnule());
					Assert.assertEquals(dateDebut, bouclement.getDateDebut());
					Assert.assertEquals(DayMonth.get(6, 30), bouclement.getAncrage());
					Assert.assertEquals(12, bouclement.getPeriodeMois());
				}

				// tâches de contrôle de dossier
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable(entreprise);
				criterion.setInclureTachesAnnulees(true);
				criterion.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(criterion);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				final Tache tache = tachesControle.get(0);
				Assert.assertEquals(TacheControleDossier.class, tache.getClass());
				Assert.assertEquals("Retour DI - Changement de période fiscale", tache.getCommentaire());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertFalse(tache.isAnnule());

				// et finalement les déclarations

				// aucune déclaration sur la période d'avant
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeInitiale, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(0, declarations.size());
				}

				// et une sur la période d'après
				{
					final List<DeclarationImpotOrdinairePM> declarations = entreprise.getDeclarationsDansPeriode(DeclarationImpotOrdinairePM.class, anneeInitiale + 1, true);
					Assert.assertNotNull(declarations);
					Assert.assertEquals(1, declarations.size());
					{
						final DeclarationImpotOrdinairePM di = declarations.get(0);
						Assert.assertNotNull(di);
						Assert.assertFalse(di.isAnnule());
						Assert.assertEquals(dateDebut, di.getDateDebut());
						Assert.assertEquals(dateDebut, di.getDateDebutExerciceCommercial());
						Assert.assertEquals(date(anneeInitiale + 1, 1, 31), di.getDateFin());
						Assert.assertEquals(date(anneeInitiale + 1, 1, 31), di.getDateFinExerciceCommercial());
						Assert.assertEquals((Integer) 1, di.getNumero());
						Assert.assertNotNull(di.getModeleDocument());
						Assert.assertSame(di.getPeriode(), di.getModeleDocument().getPeriodeFiscale());
					}
				}
			}
		});
	}

	/**
	 * SIFISC-22254 : un exercice commercial 1.7.2015 -> 30.06.2016 est tiré par la DI jusqu'en 2017
	 * (cela devrait poser un problème car alors 2016 n'a pas de bouclement alors qu'il ne s'agit pas de la première année)
	 * --> cas où c'est la première DI qui est décalée, d'une année de trop...
	 */
	@Test
	public void testTraitementDecalagePremierBouclementAnneeApresSuivante() throws Exception {

		final int anneeInitiale = RegDate.get().year() - 3;
		final RegDate dateDebut = RegDate.get(anneeInitiale, 1, 1);

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addRaisonSociale(entreprise, dateDebut, null, "Entreprise Dugenou");
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebut, DayMonth.get(6, 30), 12);      // initialement, bouclements tous les ans au 30.06
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bussigny);

				final PeriodeFiscale pfCourante = addPeriodeFiscale(anneeInitiale);
				final PeriodeFiscale pfSuivante = addPeriodeFiscale(anneeInitiale + 1);
				addPeriodeFiscale(anneeInitiale + 2);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfCourante);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfSuivante);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfCourante, date(anneeInitiale, 1, 1), date(anneeInitiale, 6, 30), date(anneeInitiale, 1, 1), date(anneeInitiale, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(anneeInitiale, 7, 6));
				addEtatDeclarationRetournee(di, date(anneeInitiale + 1, 1, 20));

				return entreprise.getNumero();
			}
		});

		// arrivée du contenu de la DI avec un changement de PF -> année + 2 (l'ancienne PF de la DI se retrouve alors sans bouclement du tout, ce qui n'est pas grave, puisque c'est
		// la première, mais c'est un souci pour la suivante...)
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(date(anneeInitiale + 2, 1, 31), null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pm, anneeInitiale, 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		// - la DI n'a pas changé de PF ni de dates
		// - les bouclements n'ont pas été modifiés
		// - une tâche de contrôle de dossier a été générée
		// - une remarque a été ajoutée
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals(String.format("Le retour de la DI %d/%d annonce une nouvelle fin d'exercice commercial au %s, mais l'année civile %d se retrouve alors sans bouclement, ce qui est interdit.",
				                                  anneeInitiale,
				                                  1,
				                                  RegDateHelper.dateToDisplayString(date(anneeInitiale + 2, 1, 31)),
				                                  anneeInitiale + 1),
				                    remarque.getTexte());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Date de fin de l'exercice commercial", tache.getCommentaire());
				}

				final List<DeclarationImpotOrdinairePM> dis = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true);
				Assert.assertNotNull(dis);
				Assert.assertEquals(1, dis.size());
				final DeclarationImpotOrdinairePM di = dis.get(0);
				Assert.assertNotNull(di);
				Assert.assertFalse(di.isAnnule());
				Assert.assertEquals(dateDebut, di.getDateDebut());
				Assert.assertEquals(dateDebut, di.getDateDebutExerciceCommercial());
				Assert.assertEquals(date(anneeInitiale, 6, 30), di.getDateFin());
				Assert.assertEquals(date(anneeInitiale, 6, 30), di.getDateFinExerciceCommercial());

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);

				// je ne compare que les 4 premiers exercices, car un 5ème apparaît parfois dans le calcul (quand la date du jour est en fin d'année, i.e. après le 1.7 de l'année)
				Assert.assertTrue(String.valueOf(exercices.size()), exercices.size() == 4 || exercices.size() == 5);
				Assert.assertEquals(new ExerciceCommercial(dateDebut, date(dateDebut.year(), 6, 30)), exercices.get(0));
				Assert.assertEquals(new ExerciceCommercial(date(dateDebut.year(), 7, 1), date(anneeInitiale + 1, 6, 30)), exercices.get(1));
				Assert.assertEquals(new ExerciceCommercial(date(anneeInitiale + 1, 7, 1), date(anneeInitiale + 2, 6, 30)), exercices.get(2));
				Assert.assertEquals(new ExerciceCommercial(date(anneeInitiale + 2, 7, 1), date(anneeInitiale + 3, 6, 30)), exercices.get(3));
			}
		});
	}

	/**
	 * SIFISC-22462 la date d'exercice commercial n'a pas été déplacée malgré l'arrivée d'une DI qui va bien
	 * (cas de l'exercice commercial déplacé plus loin dans le futur que la prochaine date de bouclement précédemment connue)
	 * Exemple :
	 * <ul>
	 *     <li>bouclements initiaux tous les 30.09</li>
	 *     <li>DI initiale jusqu'au 30.09.XXXX</li>
	 *     <li>retour de la DI avec nouvelle date de fin au 31.12 de l'année suivante (= après le 30.09.(XXXX + 1))</li>
	 * </ul>
	 */
	@Test
	public void testTraitementDecalageExerciceCommercial() throws Exception {

		final RegDate dateDebut = date(2016, 4, 27);

		final long pm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Ma grande entreprise");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebut, DayMonth.get(9, 30), 12);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bussigny);

				final PeriodeFiscale pfInitiale = addPeriodeFiscale(dateDebut.year());
				final PeriodeFiscale pfFinale = addPeriodeFiscale(dateDebut.year() + 1);
				final ModeleDocument mdInitial = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfInitiale);
				final ModeleDocument mdFinal = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfFinale);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfInitiale, dateDebut, date(dateDebut.year(), 9, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdInitial);
				addEtatDeclarationEmise(di, date(dateDebut.year(), 10, 5));
				addEtatDeclarationRetournee(di, date(dateDebut.year() + 1, 1, 1));

				return entreprise.getNumero();
			}
		});

		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(date(dateDebut.year() + 1, 12, 31), null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pm, dateDebut.year(), 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		// - la DI a été déplacée en 2017 (-> 31.12)
		// - le premier bouclement est au 31.12.2017
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals(String.format("La déclaration %d/%d a été transformée en %d/%d suite au déplacement de la date de fin d'exercice commercial du 30.09.2016 au 31.12.2017 par retour de la DI.",
				                                  dateDebut.year(),
				                                  1,
				                                  dateDebut.year() + 1,
				                                  1),
				                    remarque.getTexte());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Changement de période fiscale", tache.getCommentaire());
				}

				final List<DeclarationImpotOrdinairePM> dis = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true);
				Assert.assertNotNull(dis);
				Assert.assertEquals(1, dis.size());
				final DeclarationImpotOrdinairePM di = dis.get(0);
				Assert.assertNotNull(di);
				Assert.assertFalse(di.isAnnule());
				Assert.assertEquals(dateDebut, di.getDateDebut());
				Assert.assertEquals(dateDebut, di.getDateDebutExerciceCommercial());
				Assert.assertEquals(date(dateDebut.year() + 1, 12, 31), di.getDateFin());
				Assert.assertEquals(date(dateDebut.year() + 1, 12, 31), di.getDateFinExerciceCommercial());

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);
				Assert.assertTrue(String.valueOf(exercices.size()), exercices.size() > 0);
				final ExerciceCommercial exercice = exercices.get(0);
				Assert.assertNotNull(exercice);
				Assert.assertEquals(dateDebut, exercice.getDateDebut());
				Assert.assertEquals(date(dateDebut.year() + 1, 12, 31), exercice.getDateFin());
			}
		});
	}

	/**
	 * SIFISC-22459 : la date d'exercice commercial annoncée par la DI est, par erreur (faute de frappe ?) antérieure à la date de début de la DI
	 * (il ne faut alors pas en tenir compte...)
	 */
	@Test
	public void testFinExerciceCommercialAnnonceeAnterieureADebutConnu() throws Exception {

		final RegDate dateDebut = date(2016, 4, 27);

		final long pm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Ma grande entreprise");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebut, DayMonth.get(9, 30), 12);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bussigny);

				final PeriodeFiscale pfFinale = addPeriodeFiscale(dateDebut.year() - 1);
				final PeriodeFiscale pfInitiale = addPeriodeFiscale(dateDebut.year());
				final ModeleDocument mdFinal = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfFinale);
				final ModeleDocument mdInitial = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pfInitiale);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pfInitiale, dateDebut, date(dateDebut.year(), 9, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, mdInitial);
				addEtatDeclarationEmise(di, date(dateDebut.year(), 10, 5));
				addEtatDeclarationRetournee(di, date(dateDebut.year() + 1, 1, 1));

				return entreprise.getNumero();
			}
		});

		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(date(dateDebut.year() - 1, 12, 31), null, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pm, dateDebut.year(), 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification du résultat
		// - la DI n'a pas été déplacée en 2015
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals(String.format("Le retour de la DI %d/1 annonce une nouvelle fin d'exercice commercial au 31.12.2015, mais celle-ci n'a pas été prise en compte automatiquement car elle est antérieure à la date de début de l'exercice commercial de la DI (27.04.2016).",
				                                  dateDebut.year()),
				                    remarque.getTexte());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Date de fin de l'exercice commercial", tache.getCommentaire());
				}

				final List<DeclarationImpotOrdinairePM> dis = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true);
				Assert.assertNotNull(dis);
				Assert.assertEquals(1, dis.size());
				final DeclarationImpotOrdinairePM di = dis.get(0);
				Assert.assertNotNull(di);
				Assert.assertFalse(di.isAnnule());
				Assert.assertEquals(dateDebut, di.getDateDebut());
				Assert.assertEquals(dateDebut, di.getDateDebutExerciceCommercial());
				Assert.assertEquals(date(dateDebut.year(), 9, 30), di.getDateFin());
				Assert.assertEquals(date(dateDebut.year(), 9, 30), di.getDateFinExerciceCommercial());

				final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertNotNull(exercices);
				Assert.assertTrue(String.valueOf(exercices.size()), exercices.size() > 0);
				final ExerciceCommercial exercice = exercices.get(0);
				Assert.assertNotNull(exercice);
				Assert.assertEquals(dateDebut, exercice.getDateDebut());
				Assert.assertEquals(date(dateDebut.year(), 9, 30), exercice.getDateFin());
			}
		});
	}

	@Test
	public void testSurchargeAdresseExistantePosterieureADateReference() throws Exception {

		final RegDate dateDebut = date(2016, 4, 27);

		final long pm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Ma grande entreprise");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebut, DayMonth.get(9, 30), 12);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(dateDebut.year());
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebut, date(dateDebut.year(), 9, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(dateDebut.year(), 10, 5));
				addEtatDeclarationRetournee(di, date(dateDebut.year() + 1, 1, 1));

				// on ajoute une adresse dont la date de début de validité est postérieure à la date de quittance de la DI
				addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, date(dateDebut.year() + 1, 2, 14), null, MockRue.Echallens.RouteDeMoudon);

				return entreprise.getNumero();
			}
		});

        final AdresseRaisonSociale adresseCourrier = new AdresseRaisonSociale.Brutte("Ma grande entreprise", "Avenue du 14 avril 12", null, null, null, null, "1020", "Renens VD");
		final InformationsEntreprise infoEntreprise = new InformationsEntreprise(date(dateDebut.year(), 9, 30), adresseCourrier, null, null, null, null, null);
		final RetourDI retour = new RetourDI(pm, dateDebut.year(), 1, infoEntreprise, null);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals(String.format("L'adresse récupérée dans la DI %d/1 (Ma grande entreprise / Avenue du 14 avril 12 / 1020 / Renens VD) n'a pas été traitée en raison de la présence d'une surcharge d'adresse courrier existante à partir du 14.02.%d.",
				                                  dateDebut.year(), dateDebut.year() + 1),
				                    remarque.getTexte());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(1, tachesControle.size());
				{
					final Tache tache = tachesControle.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals("Retour DI - Adresse non-traitée", tache.getCommentaire());
				}

				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(1, surcharges.size());
				final AdresseTiers surcharge = surcharges.get(0);
				Assert.assertNotNull(surcharge);
				Assert.assertFalse(surcharge.isAnnule());
			}
		});
	}

	@Test
	public void testSalutationsSurAdresseMandataire() throws Exception {

		final RegDate dateDebut = date(2016, 4, 27);

		final long pm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Ma grande entreprise");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebut, DayMonth.get(9, 30), 12);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(dateDebut.year());
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, dateDebut, date(dateDebut.year(), 9, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(dateDebut.year(), 10, 5));
				addEtatDeclarationRetournee(di, date(dateDebut.year() + 1, 1, 1));

				return entreprise.getNumero();
			}
		});

		final DestinataireAdresse destinataire = new DestinataireAdresse.Personne(null, "François", "Morin", "Gérance");
		final AdresseRaisonSociale adresseCourrier = new AdresseRaisonSociale.StructureeSuisse(destinataire, "3ème étage droite", null, MockRue.Renens.QuatorzeAvril.getNoRue(), MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "25", null, MockLocalite.Renens.getNom(), MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre());
		final InformationsMandataire mandataire = new InformationsMandataire(null, adresseCourrier, Boolean.FALSE, null);
		final RetourDI retour = new RetourDI(pm, dateDebut.year(), 1, null, mandataire);

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(0, remarques.size());

				final TacheCriteria tacheCriteria = new TacheCriteria();
				tacheCriteria.setTypeTache(TypeTache.TacheControleDossier);
				final List<Tache> tachesControle = tacheDAO.find(tacheCriteria);
				Assert.assertNotNull(tachesControle);
				Assert.assertEquals(0, tachesControle.size());

				final List<AdresseTiers> surcharges = entreprise.getAdressesTiersSorted();
				Assert.assertNotNull(surcharges);
				Assert.assertEquals(0, surcharges.size());

				// une adresse mandataire a dû être créée
				final Set<AdresseMandataire> adressesMandataires = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adressesMandataires);
				Assert.assertEquals(1, adressesMandataires.size());
				final AdresseMandataire adresseMandataire = adressesMandataires.iterator().next();
				Assert.assertNotNull(adresseMandataire);
				Assert.assertFalse(adresseMandataire.isAnnule());
				Assert.assertTrue(adresseMandataire.isWithCopy());
				Assert.assertEquals(RegDate.get(), adresseMandataire.getDateDebut());
				Assert.assertNull(adresseMandataire.getDateFin());
				Assert.assertEquals(TypeMandat.GENERAL, adresseMandataire.getTypeMandat());
				Assert.assertEquals("Gérance", adresseMandataire.getCivilite());
				Assert.assertEquals("François Morin", adresseMandataire.getNomDestinataire());
				Assert.assertEquals("3ème étage droite", adresseMandataire.getComplement());
				Assert.assertEquals("25", adresseMandataire.getNumeroMaison());
				Assert.assertEquals(AdresseMandataireSuisse.class, adresseMandataire.getClass());
				final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresseMandataire;
				Assert.assertEquals(MockRue.Renens.QuatorzeAvril.getNoRue(), adresseSuisse.getNumeroRue());
				Assert.assertEquals(MockLocalite.Renens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());

				// et pas de lien de mandat
				final List<Mandat> mandats = entreprise.getRapportsSujet().stream()
						.filter(Mandat.class::isInstance)
						.map(Mandat.class::cast)
						.collect(Collectors.toList());
				Assert.assertEquals(Collections.emptyList(), mandats);
			}
		});
	}

	//SIFISC-28705 les liens mandataires  sur les APM ne doivent pas être touchées en cas de retour DI qui ne contiennent aucune infos sur les mandataires.
	@Test
	public void testAbsenceInformationMandatairePourAPM() throws Exception {

		final int annee = 2015;
		final RegDate dateDebutEntreprise = date(2010, 2, 1);
		final RegDate dateQuittance = date(annee + 1, 5, 13);

		final long idEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma petite Association qui va bien");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_APM_BATCH, pf);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(annee, 1, 1), date(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, date(annee + 1, 1, 5));
				addEtatDeclarationRetournee(di, dateQuittance);


				final Entreprise mandataire = addEntrepriseInconnueAuCivil();
				addRaisonSociale(mandataire, date(1950, 4, 2), null, "Mandataire à toute heure SA");
				addFormeJuridique(mandataire, date(1950, 4, 2), null, FormeJuridiqueEntreprise.SA);

				addMandatGeneral(entreprise, mandataire, dateDebutEntreprise, null, true);

				return entreprise.getNumero();
			}
		});




		// réception des données de retour (en particulier, pas de mandataire)
		final RetourDI retour = new RetourDI(idEntreprise, annee, 1, null, null);
		final RegDate dateTraitement = RegDate.get();

		// traitement de ces données
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				service.traiterRetour(retour, Collections.emptyMap());
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idEntreprise);
				Assert.assertNotNull(entreprise);

				final List<Mandat> mandats = new ArrayList<>();
				for (RapportEntreTiers ret : entreprise.getRapportsSujet()) {
					if (ret instanceof Mandat) {
						mandats.add((Mandat) ret);
					}
				}
				Assert.assertEquals(1, mandats.size());
				final Mandat mandat = mandats.get(0);
				Assert.assertNotNull(mandat);
				Assert.assertFalse(mandat.isAnnule());
				Assert.assertEquals(dateDebutEntreprise, mandat.getDateDebut());
				Assert.assertNull(mandat.getDateFin());
				Assert.assertEquals(TypeMandat.GENERAL, mandat.getTypeMandat());
			}
		});
	}
}
