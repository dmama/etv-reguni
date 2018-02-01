package ch.vd.unireg.declaration.snc;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheAnnulationQuestionnaireSNC;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;
import ch.vd.unireg.validation.ValidationService;

public class DeterminationQuestionnairesSNCAEmettreProcessorTest extends BusinessTest {

	private DeterminationQuestionnairesSNCAEmettreProcessor processor;
	private ParametreAppService parametreAppService;
	private Integer premierePeriodeEnvoi;
	private TacheDAO tacheDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final PeriodeFiscaleDAO periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final ValidationService validationService = getBean(ValidationService.class, "validationService");
		final QuestionnaireSNCService qsncService = getBean(QuestionnaireSNCService.class, "qsncService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		processor = new DeterminationQuestionnairesSNCAEmettreProcessor(parametreAppService, transactionManager, periodeFiscaleDAO, hibernateTemplate, tiersService, adresseService, validationService, tacheDAO, qsncService);

		// on place la première période d'envoi des questionnaires SNC en 2013 pour faciliter les tests
		premierePeriodeEnvoi = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(2013);
	}

	@Override
	public void onTearDown() throws Exception {
		// reset du paramètre modifié pour le test
		if (premierePeriodeEnvoi != null) {
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(premierePeriodeEnvoi);
		}
		super.onTearDown();
	}

	@Test
	public void testPeriodeFiscaleEtDateTraitement() throws Exception {

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				addPeriodeFiscale(2012);
				addPeriodeFiscale(2014);
			}
		});

		try {
			processor.run(2014, RegDate.get(2013, 4, 12), 1, null);
			Assert.fail("La date de traitement est avant le début de la PF demandée");
		}
		catch (DeclarationException e) {
			Assert.assertEquals("La période fiscale 2014 n'est pas échue à la date de traitement [12.04.2013].", e.getMessage());
		}
		try {
			processor.run(2014, RegDate.get(2014, 12, 31), 1, null);
			Assert.fail("La date de traitement est avant le a fin de la PF demandée");
		}
		catch (DeclarationException e) {
			Assert.assertEquals("La période fiscale 2014 n'est pas échue à la date de traitement [31.12.2014].", e.getMessage());
		}
		try {
			processor.run(2012, RegDate.get(2014, 12, 31), 1, null);
			Assert.fail("La PF demandée est bien trop loin dans le passé");
		}
		catch (DeclarationException e) {
			Assert.assertEquals("La période fiscale 2012 est antérieure à la première période fiscale d'envoi de documents PM par Unireg [2013].", e.getMessage());
		}

		// et finalement un tir qui n'explose pas
		final DeterminationQuestionnairesSNCResults res = processor.run(2014, RegDate.get(2015, 1, 1), 1, null);
		Assert.assertNotNull(res);
	}

	/**
	 * Test du cas il n'y a aucun tiers entreprise qui a des fors IRF sur la PF qui nous intéresse
	 */
	@Test
	public void testAucuneEntrepriseConcernee() throws Exception {

		// mise en place fiscale : une entreprise IBC et une personne physique avec for sur 2014
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				addPeriodeFiscale(2014);

				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société anonyme");
				addCapitalEntreprise(entreprise, dateDebutEntreprise, null, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);

				final PersonnePhysique pp = addNonHabitant("Alexandre", "Jardinnet", date(1974, 5, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 4, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(2014, RegDate.get(), 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // effectivement aucune tâche de créée
			}
		});
	}

	@Test
	public void testEntrepriseSansQuestionnaireDuTout() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addPeriodeFiscale(pf);

				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
				return entreprise.getNumero();
			}
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // rien pour l'instant
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(pmId, traite.noCtb);
		Assert.assertEquals("Ma société de personnes", traite.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, traite.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.TraiteType.EMISSION_CREE, traite.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // effectivement une tâche créée

				final Tache tache = all.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
				Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
				Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
				Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
				final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
				Assert.assertEquals(RegDate.get(pf, 1, 1), tacheEnvoi.getDateDebut());
				Assert.assertEquals(RegDate.get(pf, 12, 31), tacheEnvoi.getDateFin());
				Assert.assertEquals(CategorieEntreprise.SP, tacheEnvoi.getCategorieEntreprise());

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(0, questionnaires.size());      // pas de document généré
			}
		});
	}

	@Test
	public void testEntrepriseSansQuestionnaireEtTacheEnvoiAnnulee() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addPeriodeFiscale(pf);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
				final TacheEnvoiQuestionnaireSNC tache = addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(dateTraitement), RegDate.get(pf, 4, 1), RegDate.get(pf, 4, 12), CategorieEntreprise.SP, entreprise, oipm);
				tache.setAnnule(true);
				return entreprise.getNumero();
			}
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // la tâche générée annulée
				Assert.assertTrue(all.get(0).isAnnule());
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(pmId, traite.noCtb);
		Assert.assertEquals("Ma société de personnes", traite.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, traite.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.TraiteType.EMISSION_CREE, traite.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Collections.sort(all, new Comparator<Tache>() {
					@Override
					public int compare(Tache o1, Tache o2) {
						return Long.compare(o1.getId(), o2.getId());
					}
				});
				Assert.assertEquals(2, all.size());     // effectivement une tâche créée

				{
					// la tâche annulée du début
					final Tache tache = all.get(0);
					Assert.assertNotNull(tache);
					Assert.assertTrue(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
					Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(RegDate.get(pf, 4, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(RegDate.get(pf, 4, 12), tacheEnvoi.getDateFin());
					Assert.assertEquals(CategorieEntreprise.SP, tacheEnvoi.getCategorieEntreprise());
				}
				{
					// la nouvelle
					final Tache tache = all.get(1);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
					Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(RegDate.get(pf, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(RegDate.get(pf, 12, 31), tacheEnvoi.getDateFin());
					Assert.assertEquals(CategorieEntreprise.SP, tacheEnvoi.getCategorieEntreprise());
				}

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(0, questionnaires.size());      // pas de document généré
			}
		});
	}

	@Test
	public void testEntrepriseSansQuestionnaireEtTacheEnvoiTraitee() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addPeriodeFiscale(pf);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
				addTacheEnvoiQuestionnaireSNC(TypeEtatTache.TRAITE, Tache.getDefaultEcheance(dateTraitement), RegDate.get(pf, 4, 1), RegDate.get(pf, 4, 12), CategorieEntreprise.SP, entreprise, oipm);
				return entreprise.getNumero();
			}
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // la tâche générée annulée
				Assert.assertFalse(all.get(0).isAnnule());
				Assert.assertEquals(TypeEtatTache.TRAITE, all.get(0).getEtat());
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(pmId, traite.noCtb);
		Assert.assertEquals("Ma société de personnes", traite.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, traite.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.TraiteType.EMISSION_CREE, traite.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Collections.sort(all, new Comparator<Tache>() {
					@Override
					public int compare(Tache o1, Tache o2) {
						return Long.compare(o1.getId(), o2.getId());
					}
				});
				Assert.assertEquals(2, all.size());     // effectivement une tâche créée

				{
					// la tâche déjà traitée du début
					final Tache tache = all.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
					Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
					Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(RegDate.get(pf, 4, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(RegDate.get(pf, 4, 12), tacheEnvoi.getDateFin());
					Assert.assertEquals(CategorieEntreprise.SP, tacheEnvoi.getCategorieEntreprise());
				}
				{
					// la nouvelle
					final Tache tache = all.get(1);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
					Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(RegDate.get(pf, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(RegDate.get(pf, 12, 31), tacheEnvoi.getDateFin());
					Assert.assertEquals(CategorieEntreprise.SP, tacheEnvoi.getCategorieEntreprise());
				}

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(0, questionnaires.size());      // pas de document généré
			}
		});
	}

	@Test
	public void testEntrepriseAvecQuestionnaireMaisSurAutrePF() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf2013 = addPeriodeFiscale(2013);
				final PeriodeFiscale pf2014 = addPeriodeFiscale(pf);
				final PeriodeFiscale pf2015 = addPeriodeFiscale(2015);

				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
				addQuestionnaireSNC(entreprise, pf2013);
				addQuestionnaireSNC(entreprise, pf2015);
				return entreprise.getNumero();
			}
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // rien pour l'instant
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(pmId, traite.noCtb);
		Assert.assertEquals("Ma société de personnes", traite.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, traite.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.TraiteType.EMISSION_CREE, traite.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // effectivement une tâche créée

				final Tache tache = all.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
				Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
				Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
				Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
				final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
				Assert.assertEquals(RegDate.get(pf, 1, 1), tacheEnvoi.getDateDebut());
				Assert.assertEquals(RegDate.get(pf, 12, 31), tacheEnvoi.getDateFin());
				Assert.assertEquals(CategorieEntreprise.SP, tacheEnvoi.getCategorieEntreprise());

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(0, questionnaires.size());      // pas de document généré
			}
		});
	}

	@Test
	public void testEntrepriseSPNonActiveSurPF() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final PeriodeFiscale pf2013 = addPeriodeFiscale(2013);
			final PeriodeFiscale pf2014 = addPeriodeFiscale(pf);

			final RegDate dateDebutEntreprise = date(2008, 8, 12);
			final RegDate dateFinEntreprise = date(2013, 9, 30);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, dateFinEntreprise, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
			addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFinEntreprise, MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addQuestionnaireSNC(entreprise, pf2013);
			return entreprise.getNumero();
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // rien pour l'instant
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Ignore ignore = res.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(pmId, ignore.noCtb);
		Assert.assertEquals("Ma société de personnes", ignore.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, ignore.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.IgnoreType.AUCUN_QUESTIONNAIRE_REQUIS, ignore.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // effectivement aucune tâche de créée

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(0, questionnaires.size());      // pas de document généré
			}
		});
	}

	@Test
	public void testEntrepriseSPNonActiveSurPFSansQuestionnaireMaisTacheEnvoiPresente() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final PeriodeFiscale pf2013 = addPeriodeFiscale(2013);
			final PeriodeFiscale pf2014 = addPeriodeFiscale(pf);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			final RegDate dateDebutEntreprise = date(2008, 8, 12);
			final RegDate dateFinEntreprise = date(2013, 9, 30);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, dateFinEntreprise, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
			addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFinEntreprise, MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(dateTraitement), RegDate.get(pf, 3, 1), RegDate.get(pf, 12, 3), CategorieEntreprise.SP, entreprise, oipm);    // cette tâche est en trop!
			return entreprise.getNumero();
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // la tâche générée à la main
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(pmId, traite.noCtb);
		Assert.assertEquals("Ma société de personnes", traite.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, traite.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.TraiteType.EMISSION_ANNULEE, traite.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // toujours une seule tâche

				final Tache tache = all.get(0);
				Assert.assertNotNull(tache);
				Assert.assertTrue(tache.isAnnule());                // tâche annulée !
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
				Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
				Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
				Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
				final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
				Assert.assertEquals(RegDate.get(pf, 3, 1), tacheEnvoi.getDateDebut());
				Assert.assertEquals(RegDate.get(pf, 12, 3), tacheEnvoi.getDateFin());

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(0, questionnaires.size());      // pas de document généré
			}
		});
	}

	@Test
	public void testEntrepriseSPNonActiveSurPFMaisAvecQuestionnaire() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final PeriodeFiscale pf2014 = addPeriodeFiscale(pf);

			final RegDate dateDebutEntreprise = date(2008, 8, 12);
			final RegDate dateFinEntreprise = date(2013, 9, 30);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, dateFinEntreprise, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
			addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFinEntreprise, MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addQuestionnaireSNC(entreprise, pf2014);        // celui-ci ne devrait pas exister, en fait...
			return entreprise.getNumero();
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // rien pour l'instant
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(pmId, traite.noCtb);
		Assert.assertEquals("Ma société de personnes", traite.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, traite.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.TraiteType.ANNULATION_CREE, traite.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // effectivement une tâche créée

				final Tache tache = all.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
				Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
				Assert.assertEquals(TypeTache.TacheAnnulationQuestionnaireSNC, tache.getTypeTache());
				Assert.assertEquals(TacheAnnulationQuestionnaireSNC.class, tache.getClass());
				final TacheAnnulationQuestionnaireSNC tacheAnnulation = (TacheAnnulationQuestionnaireSNC) tache;
				Assert.assertEquals(RegDate.get(pf, 1, 1), tacheAnnulation.getDeclaration().getDateDebut());
				Assert.assertEquals(RegDate.get(pf, 12, 31), tacheAnnulation.getDeclaration().getDateFin());

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());
				Assert.assertFalse(questionnaires.get(0).isAnnule());       // la tâche n'est pas traitée, le document non-annulé
			}
		});
	}

	@Test
	public void testEntrepriseSPNonActiveSurPFMaisAvecQuestionnaireEtTacheAnnulation() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf2014 = addPeriodeFiscale(pf);

				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final RegDate dateFinEntreprise = date(2013, 9, 30);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFinEntreprise, MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf2014);        // celui-ci ne devrait pas exister, en fait...
				addTacheAnnulationQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(dateTraitement), questionnaire, entreprise, oipm);       // ça tombe bien, la tâche d'annulation est déjà là
				return entreprise.getNumero();
			}
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // la seule tâche générée au départ
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Ignore ignore = res.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(pmId, ignore.noCtb);
		Assert.assertEquals("Ma société de personnes", ignore.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, ignore.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.IgnoreType.TACHE_ANNULATION_DEJA_PRESENTE, ignore.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // effectivement toujours une seule tâche (= celle du début)

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());
				Assert.assertFalse(questionnaires.get(0).isAnnule());       // la tâche n'est pas traitée, le document non-annulé
			}
		});
	}

	@Test
	public void testEntrepriseAvecQuestionnaireDejaPresent() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf2013 = addPeriodeFiscale(2013);
				final PeriodeFiscale pf2014 = addPeriodeFiscale(pf);
				final PeriodeFiscale pf2015 = addPeriodeFiscale(2015);

				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
				addQuestionnaireSNC(entreprise, pf2013);
				addQuestionnaireSNC(entreprise, pf2014);
				addQuestionnaireSNC(entreprise, pf2015);
				return entreprise.getNumero();
			}
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // rien pour l'instant
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Ignore ignore = res.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(pmId, ignore.noCtb);
		Assert.assertEquals("Ma société de personnes", ignore.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, ignore.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.IgnoreType.QUESTIONNAIRE_DEJA_PRESENT, ignore.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // effectivement aucune tâche de créée

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());
				Assert.assertFalse(questionnaires.get(0).isAnnule());       // le document est toujours là, non-annulé
			}
		});
	}

	@Test
	public void testEntrepriseAvecQuestionnaireDejaPresentMaisTacheAnnulationAussi() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf2014 = addPeriodeFiscale(pf);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf2014);
				addTacheAnnulationQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(dateTraitement), questionnaire, entreprise, oipm);       // cette tâche est en trop !
				return entreprise.getNumero();
			}
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // la tâche générée
				Assert.assertFalse(all.get(0).isAnnule());
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(pmId, traite.noCtb);
		Assert.assertEquals("Ma société de personnes", traite.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, traite.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.TraiteType.ANNULATION_ANNULEE, traite.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());     // toujours la même
				Assert.assertTrue(all.get(0).isAnnule());   // mais annulée, maintenant

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());
				Assert.assertFalse(questionnaires.get(0).isAnnule());       // le document est toujours là, non-annulé
			}
		});
	}

	@Test
	public void testEntrepriseSansQuestionnaireMaisTacheEnvoiDejaPresente() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addPeriodeFiscale(pf);

				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
				final RegDate dateDebutEntreprise = date(2008, 8, 12);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SNC);
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.SOCIETE_PERS);
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
				addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(dateTraitement), RegDate.get(pf, 3, 12), RegDate.get(pf, 12, 3), CategorieEntreprise.SP, entreprise, oipm);
				addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(dateTraitement), RegDate.get(pf, 1, 5), RegDate.get(pf, 2, 3), CategorieEntreprise.SP, entreprise, oipm);     // une autre tâche bidon
				return entreprise.getNumero();
			}
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(2, all.size());     // les tâches ajoutées plus haut
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final DeterminationQuestionnairesSNCResults.Ignore ignore = res.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(pmId, ignore.noCtb);
		Assert.assertEquals("Ma société de personnes", ignore.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, ignore.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.IgnoreType.TACHE_ENVOI_DEJA_PRESENTE, ignore.type);

		final DeterminationQuestionnairesSNCResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(pmId, traite.noCtb);
		Assert.assertEquals("Ma société de personnes", traite.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, traite.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.TraiteType.EMISSION_ANNULEE, traite.type);

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(2, all.size());     // toujours les deux mêmes tâches
				Collections.sort(all, new Comparator<Tache>() {
					@Override
					public int compare(Tache o1, Tache o2) {
						return Long.compare(o1.getId(), o2.getId());
					}
				});

				{
					final Tache tache = all.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
					Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(RegDate.get(pf, 1, 1), tacheEnvoi.getDateDebut());      // date modifiée
					Assert.assertEquals(RegDate.get(pf, 12, 31), tacheEnvoi.getDateFin());      // date modifiée
					Assert.assertEquals(CategorieEntreprise.SP, tacheEnvoi.getCategorieEntreprise());
				}
				{
					final Tache tache = all.get(1);
					Assert.assertNotNull(tache);
					Assert.assertTrue(tache.isAnnule());                                        // tâche annulée
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals(Tache.getDefaultEcheance(dateTraitement), tache.getDateEcheance());
					Assert.assertEquals((Long) pmId, tache.getContribuable().getNumero());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(RegDate.get(pf, 1, 5), tacheEnvoi.getDateDebut());
					Assert.assertEquals(RegDate.get(pf, 2, 3), tacheEnvoi.getDateFin());
					Assert.assertEquals(CategorieEntreprise.SP, tacheEnvoi.getCategorieEntreprise());
				}

				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(0, questionnaires.size());      // pas de document généré
			}
		});
	}

	/**
	 * C'est le cas typique d'une entreprise sans forme juridique, au moins temporairement...
	 */
	@Test
	public void testEntrepriseAvecMauvaiseCategorieEntreprise() throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final int pf = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			addPeriodeFiscale(pf);

			final RegDate dateDebutEntreprise = date(2008, 8, 12);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebutEntreprise, date(2013, 1, 2), FormeJuridiqueEntreprise.SNC);     // forme juridique terminée
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, date(2013, 1, 2), MockTypeRegimeFiscal.SOCIETE_PERS); // régime fiscal terminé en 2013
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma société de personnes");
			addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			return entreprise.getNumero();
		});

		// vérification en base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> all = tacheDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());     // rien pour le moment...
			}
		});

		// lancement du processeur pour la PF 2014
		final DeterminationQuestionnairesSNCResults res = processor.run(pf, dateTraitement, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getNbContribuablesInspectes());
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// ignoré en raison de la catégorie d'entreprise qui est mauvaise
		final DeterminationQuestionnairesSNCResults.Ignore ignore = res.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(pmId, ignore.noCtb);
		Assert.assertEquals("Ma société de personnes", ignore.nomCtb);
		Assert.assertEquals((Integer) ServiceInfrastructureService.noOIPM, ignore.officeImpotID);
		Assert.assertEquals(DeterminationQuestionnairesSNCResults.IgnoreType.AUCUN_QUESTIONNAIRE_REQUIS, ignore.type);
		Assert.assertNull(ignore.details);
	}
}
