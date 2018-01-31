package ch.vd.uniregctb.declaration.snc;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.QuestionnaireSNCDAO;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

public class EnvoiRappelsQuestionnairesSNCProcessorTest extends BusinessTest {

	private EnvoiRappelsQuestionnairesSNCProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		final QuestionnaireSNCDAO qsncDAO = getBean(QuestionnaireSNCDAO.class, "questionnaireSNCDAO");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final QuestionnaireSNCService qsncService = getBean(QuestionnaireSNCService.class, "qsncService");
		processor = new EnvoiRappelsQuestionnairesSNCProcessor(transactionManager, hibernateTemplate, qsncDAO, delaisService, qsncService);
	}

	@Test
	public void testQuestionnaireRetourne() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
			addEtatDeclarationEmise(questionnaire, RegDate.get());
			addEtatDeclarationRetournee(questionnaire, RegDate.get());
			addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addYears(1);
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, null, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());

				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(2, questionnaire.getEtatsDeclaration().size());
				Assert.assertEquals(TypeEtatDocumentFiscal.RETOURNE, questionnaire.getDernierEtatDeclaration().getEtat());
			}
		});
	}

	@Test
	public void testQuestionnaireEmisDelaiOfficielNonEchu() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
			addEtatDeclarationEmise(questionnaire, RegDate.get());
			addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addMonths(4);      // avant le délai officiel (6 mois)
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, null, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());

				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(1, questionnaire.getEtatsDeclaration().size());
				Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, questionnaire.getDernierEtatDeclaration().getEtat());
			}
		});
	}

	@Test
	public void testQuestionnaireEmisDelaiAdministratifNonEchu() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
			addEtatDeclarationEmise(questionnaire, RegDate.get());
			addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addMonths(6).addDays(4);      // avant le délai officiel + administratif (6 mois + 15 jours)
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, null, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		final EnvoiRappelsQuestionnairesSNCResults.RappelIgnore ignore = results.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(pm, ignore.noCtb);
		Assert.assertEquals(EnvoiRappelsQuestionnairesSNCResults.CauseIgnorance.DELAI_ADMINISTRATIF_NON_ECHU, ignore.cause);
		Assert.assertEquals(periode, ignore.pf);
		Assert.assertNull(ignore.detail);

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());

				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(1, questionnaire.getEtatsDeclaration().size());
				Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, questionnaire.getDernierEtatDeclaration().getEtat());
			}
		});
	}

	@Test
	public void testQuestionnaireEmisDelaiAdministratifEchu() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
			addEtatDeclarationEmise(questionnaire, RegDate.get());
			addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addMonths(7);      // après le délai
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, null, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		final EnvoiRappelsQuestionnairesSNCResults.RappelEmis emis = results.getRappelsEmis().get(0);
		Assert.assertNotNull(emis);
		Assert.assertEquals(pm, emis.noCtb);
		Assert.assertEquals(periode, emis.pf);

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());

				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(2, questionnaire.getEtatsDeclaration().size());
				Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, questionnaire.getDernierEtatDeclaration().getEtat());
			}
		});
	}

	@Test
	public void testPlusieursQuestionnaires() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode1 = 2014;
		final int periode2 = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			{
				final PeriodeFiscale pf = addPeriodeFiscale(periode1);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
				addEtatDeclarationEmise(questionnaire, RegDate.get());
				addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			}
			{
				final PeriodeFiscale pf = addPeriodeFiscale(periode2);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
				addEtatDeclarationEmise(questionnaire, RegDate.get());
				addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			}

			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addMonths(7);      // après le délai
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, null, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(2, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		{
			final EnvoiRappelsQuestionnairesSNCResults.RappelEmis emis = results.getRappelsEmis().get(0);
			Assert.assertNotNull(emis);
			Assert.assertEquals(pm, emis.noCtb);
			Assert.assertEquals(periode1, emis.pf);
		}
		{
			final EnvoiRappelsQuestionnairesSNCResults.RappelEmis emis = results.getRappelsEmis().get(1);
			Assert.assertNotNull(emis);
			Assert.assertEquals(pm, emis.noCtb);
			Assert.assertEquals(periode2, emis.pf);
		}

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				{
					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode1, true);
					Assert.assertNotNull(questionnaires);
					Assert.assertEquals(1, questionnaires.size());

					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(2, questionnaire.getEtatsDeclaration().size());
					Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, questionnaire.getDernierEtatDeclaration().getEtat());
				}
				{
					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode2, true);
					Assert.assertNotNull(questionnaires);
					Assert.assertEquals(1, questionnaires.size());

					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(2, questionnaire.getEtatsDeclaration().size());
					Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, questionnaire.getDernierEtatDeclaration().getEtat());
				}
			}
		});
	}

	@Test
	public void testPlusieursQuestionnairesAvecLimiteNonLimitante() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode1 = 2014;
		final int periode2 = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			{
				final PeriodeFiscale pf = addPeriodeFiscale(periode1);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
				addEtatDeclarationEmise(questionnaire, RegDate.get());
				addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			}
			{
				final PeriodeFiscale pf = addPeriodeFiscale(periode2);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
				addEtatDeclarationEmise(questionnaire, RegDate.get());
				addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			}

			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addMonths(7);      // après le délai
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, null, 2, null);      // de toute façon, je n'en ai pas plus de deux...
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(2, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		{
			final EnvoiRappelsQuestionnairesSNCResults.RappelEmis emis = results.getRappelsEmis().get(0);
			Assert.assertNotNull(emis);
			Assert.assertEquals(pm, emis.noCtb);
			Assert.assertEquals(periode1, emis.pf);
		}
		{
			final EnvoiRappelsQuestionnairesSNCResults.RappelEmis emis = results.getRappelsEmis().get(1);
			Assert.assertNotNull(emis);
			Assert.assertEquals(pm, emis.noCtb);
			Assert.assertEquals(periode2, emis.pf);
		}

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				{
					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode1, true);
					Assert.assertNotNull(questionnaires);
					Assert.assertEquals(1, questionnaires.size());

					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(2, questionnaire.getEtatsDeclaration().size());
					Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, questionnaire.getDernierEtatDeclaration().getEtat());
				}
				{
					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode2, true);
					Assert.assertNotNull(questionnaires);
					Assert.assertEquals(1, questionnaires.size());

					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(2, questionnaire.getEtatsDeclaration().size());
					Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, questionnaire.getDernierEtatDeclaration().getEtat());
				}
			}
		});
	}

	@Test
	public void testPlusieursQuestionnairesAvecLimiteLimitante() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode1 = 2014;
		final int periode2 = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			{
				final PeriodeFiscale pf = addPeriodeFiscale(periode1);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
				addEtatDeclarationEmise(questionnaire, RegDate.get());
				addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			}
			{
				final PeriodeFiscale pf = addPeriodeFiscale(periode2);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
				addEtatDeclarationEmise(questionnaire, RegDate.get());
				addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			}

			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addMonths(7);      // après le délai
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, null, 1, null);      // et pourtant, il y en a deux
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		{
			final EnvoiRappelsQuestionnairesSNCResults.RappelEmis emis = results.getRappelsEmis().get(0);
			Assert.assertNotNull(emis);
			Assert.assertEquals(pm, emis.noCtb);
			Assert.assertEquals(periode1, emis.pf);
		}

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				{
					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode1, true);
					Assert.assertNotNull(questionnaires);
					Assert.assertEquals(1, questionnaires.size());

					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(2, questionnaire.getEtatsDeclaration().size());
					Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, questionnaire.getDernierEtatDeclaration().getEtat());
				}
				{
					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode2, true);
					Assert.assertNotNull(questionnaires);
					Assert.assertEquals(1, questionnaires.size());

					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(1, questionnaire.getEtatsDeclaration().size());
					Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, questionnaire.getDernierEtatDeclaration().getEtat());
				}
			}
		});
	}

	@Test
	public void testPlusieursQuestionnairesAvecPeriodeFiscaleExplicite() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode1 = 2014;
		final int periode2 = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			{
				final PeriodeFiscale pf = addPeriodeFiscale(periode1);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
				addEtatDeclarationEmise(questionnaire, RegDate.get());
				addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			}
			{
				final PeriodeFiscale pf = addPeriodeFiscale(periode2);
				final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
				addEtatDeclarationEmise(questionnaire, RegDate.get());
				addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			}

			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addMonths(7);      // après le délai
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, periode1, 1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		{
			final EnvoiRappelsQuestionnairesSNCResults.RappelEmis emis = results.getRappelsEmis().get(0);
			Assert.assertNotNull(emis);
			Assert.assertEquals(pm, emis.noCtb);
			Assert.assertEquals(periode1, emis.pf);
		}

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				{
					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode1, true);
					Assert.assertNotNull(questionnaires);
					Assert.assertEquals(1, questionnaires.size());

					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(2, questionnaire.getEtatsDeclaration().size());
					Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, questionnaire.getDernierEtatDeclaration().getEtat());
				}
				{
					final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode2, true);
					Assert.assertNotNull(questionnaires);
					Assert.assertEquals(1, questionnaires.size());

					final QuestionnaireSNC questionnaire = questionnaires.get(0);
					Assert.assertNotNull(questionnaire);
					Assert.assertFalse(questionnaire.isAnnule());
					Assert.assertEquals(1, questionnaire.getEtatsDeclaration().size());
					Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, questionnaire.getDernierEtatDeclaration().getEtat());
				}
			}
		});
	}

	@Test
	public void testQuestionnaireEmisDelaiAdministratifEchuMaisForDeplace() throws Exception {

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode = 2015;

		// mise en place fiscale
		final long pm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, date(periode - 1, 12, 31), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);   // il n'y a plus de for sur 2015...

			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
			addEtatDeclarationEmise(questionnaire, RegDate.get());
			addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);
			return entreprise.getNumero();
		});

		// lancement du processus de rappel
		final RegDate dateTraitement = RegDate.get().addMonths(7);      // après le délai
		final EnvoiRappelsQuestionnairesSNCResults results = processor.run(dateTraitement, null, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getRappelsEmis().size());
		Assert.assertFalse(results.wasInterrupted());

		final EnvoiRappelsQuestionnairesSNCResults.RappelIgnore ignore = results.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(pm, ignore.noCtb);
		Assert.assertEquals(EnvoiRappelsQuestionnairesSNCResults.CauseIgnorance.QUESTIONNAIRE_DEVRAIT_ETRE_ANNULE, ignore.cause);
		Assert.assertEquals(periode, ignore.pf);
		Assert.assertNull(ignore.detail);

		// vérification de l'état du questionnaire en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pm);
				Assert.assertNotNull(entreprise);

				final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode, true);
				Assert.assertNotNull(questionnaires);
				Assert.assertEquals(1, questionnaires.size());

				final QuestionnaireSNC questionnaire = questionnaires.get(0);
				Assert.assertNotNull(questionnaire);
				Assert.assertFalse(questionnaire.isAnnule());
				Assert.assertEquals(1, questionnaire.getEtatsDeclaration().size());
				Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, questionnaire.getDernierEtatDeclaration().getEtat());
			}
		});
	}
}
