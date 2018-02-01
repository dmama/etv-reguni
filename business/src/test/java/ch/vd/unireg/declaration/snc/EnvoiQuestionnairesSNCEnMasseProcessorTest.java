package ch.vd.uniregctb.declaration.snc;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDeclarationRappelable;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class EnvoiQuestionnairesSNCEnMasseProcessorTest extends BusinessTest {

	private TacheDAO tacheDAO;
	private EnvoiQuestionnairesSNCEnMasseProcessor processor;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		final QuestionnaireSNCService questionnaireSNCService = getBean(QuestionnaireSNCService.class, "qsncService");
		final PeriodeFiscaleDAO periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final TicketService ticketService = getBean(TicketService.class, "ticketService");
		processor = new EnvoiQuestionnairesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService, tacheDAO, questionnaireSNCService, periodeFiscaleDAO, ticketService);
	}

	@Test
	public void testAucuneTache() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			return entreprise.getNumero();
		});

		// lancement du job
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, RegDate.get(), null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification qu'aucun questionnaire SNC n'a effectivement été généré
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(0, entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true).size());
				Assert.assertEquals(0, entreprise.getDocumentsFiscaux().size());
			}
		});
	}

	@Test
	public void testTacheEnInstanceAnnulee() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			final TacheEnvoiQuestionnaireSNC tache = addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise, oipm);
			tache.setAnnule(true);

			return entreprise.getNumero();
		});

		// lancement du job
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, RegDate.get(), null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification qu'aucun questionnaire SNC n'a effectivement été généré
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(0, entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true).size());
				Assert.assertEquals(0, entreprise.getDocumentsFiscaux().size());
			}
		});
	}

	@Test
	public void testTacheDejaTaitee() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.TRAITE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise, oipm);

			return entreprise.getNumero();
		});

		// lancement du job
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, RegDate.get(), null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification qu'aucun questionnaire SNC n'a effectivement été généré
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(0, entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true).size());
				Assert.assertEquals(0, entreprise.getDocumentsFiscaux().size());
			}
		});
	}

	@Test
	public void testTacheEnInstanceMaisQuestionnaireDejaPresent() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 2, 1), date(periode, 10, 31), CategorieEntreprise.SP, entreprise, oipm);

			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf, date(periode, 4, 12), date(periode, 7, 23));        // dans le cas d'un questionnaire existant, on ne ré-aligne rien, a priori
			addEtatDeclarationEmise(questionnaire, RegDate.get().addMonths(-6));
			addDelaiDeclaration(questionnaire, RegDate.get().addMonths(-6), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);

			return entreprise.getNumero();
		});

		// lancement du job
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, RegDate.get(), null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getNombreEnvoyes());
		Assert.assertEquals(1, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		final EnvoiQuestionnairesSNCEnMasseResults.ContribuableIgnore ignore = results.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(pmId, ignore.noCtb);
		Assert.assertEquals(EnvoiQuestionnairesSNCEnMasseResults.CauseIgnorance.QUESTIONNAIRE_DEJA_EXISTANT, ignore.cause);

		// vérification qu'aucun questionnaire SNC n'a effectivement été généré en plus, mais que les dates du questionnaire existant ont bien été ré-alignées
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());

				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(date(periode, 4, 12), questionnaire.getDateDebut());        // les dates du questionnaire existant n'ont pas été ré-alignées
				Assert.assertEquals(date(periode, 7, 23), questionnaire.getDateFin());

				// et la tâche elle-même doit être annulée
				final List<Tache> taches = tacheDAO.find(pmId);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertTrue(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
				Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
				final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
				Assert.assertEquals(date(periode, 2, 1), tacheEnvoi.getDateDebut());
				Assert.assertEquals(date(periode, 10, 31), tacheEnvoi.getDateFin());
			}
		});
	}

	@Test
	public void testTacheEnInstanceAutrePeriode() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode - 1, 1, 1), date(periode - 1, 12, 31), CategorieEntreprise.SP, entreprise, oipm);

			return entreprise.getNumero();
		});

		// lancement du job
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, RegDate.get(), null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification qu'aucun questionnaire SNC n'a effectivement été généré
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(0, entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true).size());
				Assert.assertEquals(0, entreprise.getDocumentsFiscaux().size());
			}
		});
	}

	@Test
	public void testTacheEnInstanceValideMaisMaxEnvoisZero() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise, oipm);
			addPeriodeFiscale(periode);
			return entreprise.getNumero();
		});

		// lancement du job
		final RegDate dateTraitement = RegDate.get().addMonths(-1);
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, dateTraitement, 0, null);       // max envois à 0
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification qu'aucun questionnaire SNC n'a effectivement été généré
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(0, entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true).size());
				Assert.assertEquals(0, entreprise.getDocumentsFiscaux().size());
			}
		});
	}

	@Test
	public void testTacheEnInstanceValide() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise, oipm);
			addPeriodeFiscale(periode);
			return entreprise.getNumero();
		});

		// lancement du job
		final RegDate dateTraitement = RegDate.get().addMonths(-1);
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, dateTraitement, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification du numéro de contribuable
		final EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye envoye = results.getEnvoyes().get(0);
		Assert.assertNotNull(envoye);
		Assert.assertEquals(pmId, envoye.noCtb);

		// vérification qu'un questionnaire SNC a effectivement été généré
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertEquals(1, questionnaires.size());

				// le questionnaire lui-même
				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(date(periode, 1, 1), questionnaire.getDateDebut());
				Assert.assertEquals(date(periode, 12, 31), questionnaire.getDateFin());
				Assert.assertEquals(date(periode + 1, 3, 15), questionnaire.getDelaiRetourImprime());

				// son état "EMISE"
				final Set<EtatDeclaration> etats = questionnaire.getEtatsDeclaration();
				Assert.assertNotNull(etats);
				Assert.assertEquals(1, etats.size());
				final EtatDeclaration etat = etats.iterator().next();
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
				Assert.assertEquals(dateTraitement, etat.getDateObtention());

				// son délai initial
				final Set<DelaiDeclaration> delais = questionnaire.getDelaisDeclaration();
				Assert.assertNotNull(delais);
				Assert.assertEquals(1, delais.size());
				final DelaiDeclaration delai = delais.iterator().next();
				Assert.assertNotNull(delai);
				Assert.assertFalse(delai.isAnnule());
				Assert.assertEquals(date(periode + 1, 8, 31), delai.getDelaiAccordeAu());
				Assert.assertEquals(dateTraitement, delai.getDateDemande());
				Assert.assertEquals(dateTraitement, delai.getDateTraitement());
				Assert.assertEquals(EtatDelaiDocumentFiscal.ACCORDE, delai.getEtat());
				Assert.assertNull(delai.getCleArchivageCourrier());
				Assert.assertFalse(delai.isSursis());

				// la tâche d'envoi doit être marquée comme traitée
				final List<Tache> taches = tacheDAO.find(pmId);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
				Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
				Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
				final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
				Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());
				Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());

				// événement fiscal
				final Collection<EvenementFiscal> evtsFiscaux = evenementFiscalDAO.getEvenementsFiscaux(entreprise);
				Assert.assertNotNull(evtsFiscaux);
				Assert.assertEquals(1, evtsFiscaux.size());
				final EvenementFiscal evtFiscal = evtsFiscaux.iterator().next();
				Assert.assertNotNull(evtFiscal);
				Assert.assertEquals(EvenementFiscalDeclarationRappelable.class, evtFiscal.getClass());
				final EvenementFiscalDeclarationRappelable evtFiscalDeclaration = (EvenementFiscalDeclarationRappelable) evtFiscal;
				Assert.assertFalse(evtFiscalDeclaration.isAnnule());
				Assert.assertEquals(dateTraitement, evtFiscalDeclaration.getDateValeur());
				Assert.assertEquals(EvenementFiscalDeclarationRappelable.TypeAction.EMISSION, evtFiscalDeclaration.getTypeAction());
			}
		});
	}

	@Test
	public void testPlusieursEntreprises() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		final class Ids {
			long pm1;
			long pm2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);

			final Entreprise entreprise1 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise1, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise1, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise1, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise1, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise1, oipm);

			final Entreprise entreprise2 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise2, dateDebut, null, "Tous ensemble!");
			addFormeJuridique(entreprise2, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise2, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise2, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise2, oipm);

			addPeriodeFiscale(periode);

			final Ids ids1 = new Ids();
			ids1.pm1 = entreprise1.getNumero();
			ids1.pm2 = entreprise2.getNumero();
			return ids1;
		});

		// lancement du job
		final RegDate dateTraitement = RegDate.get().addMonths(-1);
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, dateTraitement, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification des numéros de contribuable (ils sont triés par ordre croissant, = ordre de création)
		{
			final EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye envoye = results.getEnvoyes().get(0);
			Assert.assertNotNull(envoye);
			Assert.assertEquals(ids.pm1, envoye.noCtb);
		}
		{
			final EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye envoye = results.getEnvoyes().get(1);
			Assert.assertNotNull(envoye);
			Assert.assertEquals(ids.pm2, envoye.noCtb);
		}

		// vérification qu'un questionnaire SNC a effectivement été généré pour chacune des entreprises
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				{
					final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.pm1);
					Assert.assertNotNull(entreprise);

					Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
					Assert.assertEquals(1, questionnaires.size());

					// le questionnaire lui-même
					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(date(periode, 1, 1), questionnaire.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), questionnaire.getDateFin());
					Assert.assertEquals(date(periode + 1, 3, 15), questionnaire.getDelaiRetourImprime());

					// son état "EMISE"
					final Set<EtatDeclaration> etats = questionnaire.getEtatsDeclaration();
					Assert.assertNotNull(etats);
					Assert.assertEquals(1, etats.size());
					final EtatDeclaration etat = etats.iterator().next();
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
					Assert.assertEquals(dateTraitement, etat.getDateObtention());

					// son délai initial
					final Set<DelaiDeclaration> delais = questionnaire.getDelaisDeclaration();
					Assert.assertNotNull(delais);
					Assert.assertEquals(1, delais.size());
					final DelaiDeclaration delai = delais.iterator().next();
					Assert.assertNotNull(delai);
					Assert.assertFalse(delai.isAnnule());
					Assert.assertEquals(date(periode + 1, 8, 31), delai.getDelaiAccordeAu());
					Assert.assertEquals(dateTraitement, delai.getDateDemande());
					Assert.assertEquals(dateTraitement, delai.getDateTraitement());
					Assert.assertEquals(EtatDelaiDocumentFiscal.ACCORDE, delai.getEtat());
					Assert.assertNull(delai.getCleArchivageCourrier());
					Assert.assertFalse(delai.isSursis());

					// la tâche d'envoi doit être marquée comme traitée
					final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());
				}
				{
					final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.pm2);
					Assert.assertNotNull(entreprise);

					Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
					Assert.assertEquals(1, questionnaires.size());

					// le questionnaire lui-même
					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(date(periode, 1, 1), questionnaire.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), questionnaire.getDateFin());
					Assert.assertEquals(date(periode + 1, 3, 15), questionnaire.getDelaiRetourImprime());

					// son état "EMISE"
					final Set<EtatDeclaration> etats = questionnaire.getEtatsDeclaration();
					Assert.assertNotNull(etats);
					Assert.assertEquals(1, etats.size());
					final EtatDeclaration etat = etats.iterator().next();
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
					Assert.assertEquals(dateTraitement, etat.getDateObtention());

					// son délai initial
					final Set<DelaiDeclaration> delais = questionnaire.getDelaisDeclaration();
					Assert.assertNotNull(delais);
					Assert.assertEquals(1, delais.size());
					final DelaiDeclaration delai = delais.iterator().next();
					Assert.assertNotNull(delai);
					Assert.assertFalse(delai.isAnnule());
					Assert.assertEquals(date(periode + 1, 8, 31), delai.getDelaiAccordeAu());
					Assert.assertEquals(dateTraitement, delai.getDateDemande());
					Assert.assertEquals(dateTraitement, delai.getDateTraitement());
					Assert.assertEquals(EtatDelaiDocumentFiscal.ACCORDE, delai.getEtat());
					Assert.assertNull(delai.getCleArchivageCourrier());
					Assert.assertFalse(delai.isSursis());

					// la tâche d'envoi doit être marquée comme traitée
					final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());
				}
			}
		});
	}

	@Test
	public void testPlusieursEntreprisesAvecMaxEnvoisNonLimitant() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		final class Ids {
			long pm1;
			long pm2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);

			final Entreprise entreprise1 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise1, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise1, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise1, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise1, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise1, oipm);

			final Entreprise entreprise2 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise2, dateDebut, null, "Tous ensemble!");
			addFormeJuridique(entreprise2, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise2, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise2, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise2, oipm);

			addPeriodeFiscale(periode);

			final Ids ids1 = new Ids();
			ids1.pm1 = entreprise1.getNumero();
			ids1.pm2 = entreprise2.getNumero();
			return ids1;
		});

		// lancement du job
		final RegDate dateTraitement = RegDate.get().addMonths(-1);
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, dateTraitement, 2, null);       // de toute façon, il n'y en a que deux..,
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification des numéros de contribuable (ils sont triés par ordre croissant, = ordre de création)
		{
			final EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye envoye = results.getEnvoyes().get(0);
			Assert.assertNotNull(envoye);
			Assert.assertEquals(ids.pm1, envoye.noCtb);
		}
		{
			final EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye envoye = results.getEnvoyes().get(1);
			Assert.assertNotNull(envoye);
			Assert.assertEquals(ids.pm2, envoye.noCtb);
		}

		// vérification qu'un questionnaire SNC a effectivement été généré pour chacune des entreprises
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				{
					final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.pm1);
					Assert.assertNotNull(entreprise);

					Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
					Assert.assertEquals(1, questionnaires.size());

					// le questionnaire lui-même
					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(date(periode, 1, 1), questionnaire.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), questionnaire.getDateFin());
					Assert.assertEquals(date(periode + 1, 3, 15), questionnaire.getDelaiRetourImprime());

					// son état "EMISE"
					final Set<EtatDeclaration> etats = questionnaire.getEtatsDeclaration();
					Assert.assertNotNull(etats);
					Assert.assertEquals(1, etats.size());
					final EtatDeclaration etat = etats.iterator().next();
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
					Assert.assertEquals(dateTraitement, etat.getDateObtention());

					// son délai initial
					final Set<DelaiDeclaration> delais = questionnaire.getDelaisDeclaration();
					Assert.assertNotNull(delais);
					Assert.assertEquals(1, delais.size());
					final DelaiDeclaration delai = delais.iterator().next();
					Assert.assertNotNull(delai);
					Assert.assertFalse(delai.isAnnule());
					Assert.assertEquals(date(periode + 1, 8, 31), delai.getDelaiAccordeAu());
					Assert.assertEquals(dateTraitement, delai.getDateDemande());
					Assert.assertEquals(dateTraitement, delai.getDateTraitement());
					Assert.assertEquals(EtatDelaiDocumentFiscal.ACCORDE, delai.getEtat());
					Assert.assertNull(delai.getCleArchivageCourrier());
					Assert.assertFalse(delai.isSursis());

					// la tâche d'envoi doit être marquée comme traitée
					final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());
				}
				{
					final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.pm2);
					Assert.assertNotNull(entreprise);

					Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
					Assert.assertEquals(1, questionnaires.size());

					// le questionnaire lui-même
					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(date(periode, 1, 1), questionnaire.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), questionnaire.getDateFin());
					Assert.assertEquals(date(periode + 1, 3, 15), questionnaire.getDelaiRetourImprime());

					// son état "EMISE"
					final Set<EtatDeclaration> etats = questionnaire.getEtatsDeclaration();
					Assert.assertNotNull(etats);
					Assert.assertEquals(1, etats.size());
					final EtatDeclaration etat = etats.iterator().next();
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
					Assert.assertEquals(dateTraitement, etat.getDateObtention());

					// son délai initial
					final Set<DelaiDeclaration> delais = questionnaire.getDelaisDeclaration();
					Assert.assertNotNull(delais);
					Assert.assertEquals(1, delais.size());
					final DelaiDeclaration delai = delais.iterator().next();
					Assert.assertNotNull(delai);
					Assert.assertFalse(delai.isAnnule());
					Assert.assertEquals(date(periode + 1, 8, 31), delai.getDelaiAccordeAu());
					Assert.assertEquals(dateTraitement, delai.getDateDemande());
					Assert.assertEquals(dateTraitement, delai.getDateTraitement());
					Assert.assertEquals(EtatDelaiDocumentFiscal.ACCORDE, delai.getEtat());
					Assert.assertNull(delai.getCleArchivageCourrier());
					Assert.assertFalse(delai.isSursis());

					// la tâche d'envoi doit être marquée comme traitée
					final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());
				}
			}
		});
	}

	@Test
	public void testPlusieursEntreprisesAvecMaxEnvoisLimitant() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		final class Ids {
			long pm1;
			long pm2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);

			final Entreprise entreprise1 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise1, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise1, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise1, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise1, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise1, oipm);

			final Entreprise entreprise2 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise2, dateDebut, null, "Tous ensemble!");
			addFormeJuridique(entreprise2, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise2, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise2, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 1), date(periode, 12, 31), CategorieEntreprise.SP, entreprise2, oipm);

			addPeriodeFiscale(periode);

			final Ids ids1 = new Ids();
			ids1.pm1 = entreprise1.getNumero();
			ids1.pm2 = entreprise2.getNumero();
			return ids1;
		});

		// lancement du job
		final RegDate dateTraitement = RegDate.get().addMonths(-1);
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, dateTraitement, 1, null);       // deux auraient dû être générés en absence de limitation
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification des numéros de contribuable (ils sont triés par ordre croissant, = ordre de création)
		final EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye envoye = results.getEnvoyes().get(0);
		Assert.assertNotNull(envoye);
		Assert.assertEquals(ids.pm1, envoye.noCtb);

		// vérification qu'un questionnaire SNC a effectivement été généré pour chacune des entreprises
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				{
					final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.pm1);
					Assert.assertNotNull(entreprise);

					Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
					Assert.assertEquals(1, questionnaires.size());

					// le questionnaire lui-même
					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(date(periode, 1, 1), questionnaire.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), questionnaire.getDateFin());
					Assert.assertEquals(date(periode + 1, 3, 15), questionnaire.getDelaiRetourImprime());

					// son état "EMISE"
					final Set<EtatDeclaration> etats = questionnaire.getEtatsDeclaration();
					Assert.assertNotNull(etats);
					Assert.assertEquals(1, etats.size());
					final EtatDeclaration etat = etats.iterator().next();
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
					Assert.assertEquals(dateTraitement, etat.getDateObtention());

					// son délai initial
					final Set<DelaiDeclaration> delais = questionnaire.getDelaisDeclaration();
					Assert.assertNotNull(delais);
					Assert.assertEquals(1, delais.size());
					final DelaiDeclaration delai = delais.iterator().next();
					Assert.assertNotNull(delai);
					Assert.assertFalse(delai.isAnnule());
					Assert.assertEquals(date(periode + 1, 8, 31), delai.getDelaiAccordeAu());
					Assert.assertEquals(dateTraitement, delai.getDateDemande());
					Assert.assertEquals(dateTraitement, delai.getDateTraitement());
					Assert.assertEquals(EtatDelaiDocumentFiscal.ACCORDE, delai.getEtat());
					Assert.assertNull(delai.getCleArchivageCourrier());
					Assert.assertFalse(delai.isSursis());

					// la tâche d'envoi doit être marquée comme traitée
					final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());
				}
				{
					final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.pm2);
					Assert.assertNotNull(entreprise);

					Assert.assertEquals(0, entreprise.getDocumentsFiscaux().size());

					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
					Assert.assertEquals(0, questionnaires.size());

					// la tâche d'envoi doit être restée en instance
					final List<Tache> taches = tacheDAO.find(entreprise.getNumero());
					Assert.assertNotNull(taches);
					Assert.assertEquals(1, taches.size());
					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());
				}
			}
		});
	}

	@Test
	public void testTacheEnInstanceValideAvecMauvaisesDates() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			// dates à redresser : 05.04 -> 09.22 doit devenir 01.01 -> 31.12
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 4, 5), date(periode, 9, 22), CategorieEntreprise.SP, entreprise, oipm);
			addPeriodeFiscale(periode);
			return entreprise.getNumero();
		});

		// lancement du job
		final RegDate dateTraitement = RegDate.get().addMonths(-1);
		final EnvoiQuestionnairesSNCEnMasseResults results = processor.run(periode, dateTraitement, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification du numéro de contribuable
		final EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye envoye = results.getEnvoyes().get(0);
		Assert.assertNotNull(envoye);
		Assert.assertEquals(pmId, envoye.noCtb);

		// vérification qu'un questionnaire SNC a effectivement été généré
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertEquals(1, questionnaires.size());

				// le questionnaire lui-même
				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(date(periode, 1, 1), questionnaire.getDateDebut());
				Assert.assertEquals(date(periode, 12, 31), questionnaire.getDateFin());
				Assert.assertEquals(date(periode + 1, 3, 15), questionnaire.getDelaiRetourImprime());

				// son état "EMISE"
				final Set<EtatDeclaration> etats = questionnaire.getEtatsDeclaration();
				Assert.assertNotNull(etats);
				Assert.assertEquals(1, etats.size());
				final EtatDeclaration etat = etats.iterator().next();
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
				Assert.assertEquals(dateTraitement, etat.getDateObtention());

				// son délai initial
				final Set<DelaiDeclaration> delais = questionnaire.getDelaisDeclaration();
				Assert.assertNotNull(delais);
				Assert.assertEquals(1, delais.size());
				final DelaiDeclaration delai = delais.iterator().next();
				Assert.assertNotNull(delai);
				Assert.assertFalse(delai.isAnnule());
				Assert.assertEquals(date(periode + 1, 8, 31), delai.getDelaiAccordeAu());
				Assert.assertEquals(dateTraitement, delai.getDateDemande());
				Assert.assertEquals(dateTraitement, delai.getDateTraitement());
				Assert.assertEquals(EtatDelaiDocumentFiscal.ACCORDE, delai.getEtat());
				Assert.assertNull(delai.getCleArchivageCourrier());
				Assert.assertFalse(delai.isSursis());

				// la tâche d'envoi doit être marquée comme traitée
				final List<Tache> taches = tacheDAO.find(pmId);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());
				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
				Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
				Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
				Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
				final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
				Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());
				Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());
			}
		});
	}

	@Test
	public void testPlusieursTachesEnInstance() throws Exception {

		final RegDate dateDebut = date(2006, 1, 4);
		final int periode = 2015;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, date(periode, 12, 31), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 1, 3), date(periode, 4, 1), CategorieEntreprise.SP, entreprise, oipm);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 4, 5), date(periode, 9, 22), CategorieEntreprise.SP, entreprise, oipm);
			addTacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(RegDate.get()), date(periode, 9, 30), date(periode, 12, 25), CategorieEntreprise.SP, entreprise, oipm);
			addPeriodeFiscale(periode);
			return entreprise.getNumero();
		});

		// lancement du job
		final RegDate dateTraitement = RegDate.get().addMonths(-1);
		final EnvoiQuestionnairesSNCEnMasseResults results = doUnderSwitch(tacheSynchronizer, true, () -> processor.run(periode, dateTraitement, null, null));
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNombreEnvoyes());
		Assert.assertEquals(0, results.getNombreIgnores());
		Assert.assertEquals(0, results.getNombreErreurs());

		// vérification du numéro de contribuable
		final EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye envoye = results.getEnvoyes().get(0);
		Assert.assertNotNull(envoye);
		Assert.assertEquals(pmId, envoye.noCtb);

		// vérification qu'un questionnaire SNC a effectivement été généré
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				Assert.assertEquals(1, entreprise.getDocumentsFiscaux().size());

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertEquals(1, questionnaires.size());

				// le questionnaire lui-même
				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(date(periode, 1, 1), questionnaire.getDateDebut());
				Assert.assertEquals(date(periode, 12, 31), questionnaire.getDateFin());
				Assert.assertEquals(date(periode + 1, 3, 15), questionnaire.getDelaiRetourImprime());

				// son état "EMISE"
				final Set<EtatDeclaration> etats = questionnaire.getEtatsDeclaration();
				Assert.assertNotNull(etats);
				Assert.assertEquals(1, etats.size());
				final EtatDeclaration etat = etats.iterator().next();
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
				Assert.assertEquals(dateTraitement, etat.getDateObtention());

				// son délai initial
				final Set<DelaiDeclaration> delais = questionnaire.getDelaisDeclaration();
				Assert.assertNotNull(delais);
				Assert.assertEquals(1, delais.size());
				final DelaiDeclaration delai = delais.iterator().next();
				Assert.assertNotNull(delai);
				Assert.assertFalse(delai.isAnnule());
				Assert.assertEquals(date(periode + 1, 8, 31), delai.getDelaiAccordeAu());
				Assert.assertEquals(dateTraitement, delai.getDateDemande());
				Assert.assertEquals(dateTraitement, delai.getDateTraitement());
				Assert.assertEquals(EtatDelaiDocumentFiscal.ACCORDE, delai.getEtat());
				Assert.assertNull(delai.getCleArchivageCourrier());
				Assert.assertFalse(delai.isSursis());

				// une seule des trois tâches en instance doit être marquée comme traitée, les autres doivent être annulées
				final List<Tache> taches = tacheDAO.find(pmId);
				Assert.assertNotNull(taches);
				Assert.assertEquals(3, taches.size());

				// les tâches sont toujours récupérées par ID croissant : la première doit être traitée non-annulée, et les autres doivent être en instance, annulées (par le processus de recalcul des tâches).
				{
					final Tache tache = taches.get(0);
					Assert.assertNotNull(tache);
					Assert.assertFalse(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.TRAITE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 1, 1), tacheEnvoi.getDateDebut());            // les dates ont été réalignées
					Assert.assertEquals(date(periode, 12, 31), tacheEnvoi.getDateFin());
				}
				{
					final Tache tache = taches.get(1);
					Assert.assertNotNull(tache);
					Assert.assertTrue(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 4, 5), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(periode, 9, 22), tacheEnvoi.getDateFin());
				}
				{
					final Tache tache = taches.get(2);
					Assert.assertNotNull(tache);
					Assert.assertTrue(tache.isAnnule());
					Assert.assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
					Assert.assertEquals(TacheEnvoiQuestionnaireSNC.class, tache.getClass());
					Assert.assertEquals(TypeTache.TacheEnvoiQuestionnaireSNC, tache.getTypeTache());
					final TacheEnvoiQuestionnaireSNC tacheEnvoi = (TacheEnvoiQuestionnaireSNC) tache;
					Assert.assertEquals(date(periode, 9, 30), tacheEnvoi.getDateDebut());
					Assert.assertEquals(date(periode, 12, 25), tacheEnvoi.getDateFin());
				}
			}
		});
	}
}
