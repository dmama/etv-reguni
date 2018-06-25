package ch.vd.unireg.declaration.snc;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EchoirQuestionnairesSNCProcessorTest extends BusinessTest {

	private EchoirQuestionnairesSNCProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final QuestionnaireSNCService questionnaireSNCService = getBean(QuestionnaireSNCService.class, "qsncService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		this.processor = new EchoirQuestionnairesSNCProcessor(hibernateTemplate, delaisService, questionnaireSNCService, transactionManager, tiersService, adresseService);
	}

	/**
	 * Ce test vérifie qu'un questionnaire SNC émis mais non-rappelé n'est pas considéré par le processeur d'échéances des questionnaires SNC.
	 */
	@Test
	public void testQuestionnaireNonRappele() throws Exception {

		class Ids {
			Long entreprise;
			Long questionnaire;
		}
		final Ids ids = new Ids();

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode = 2015;

		doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			// un questionnaire SNC émis mais non rappelé
			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
			addEtatDeclarationEmise(questionnaire, RegDate.get());
			addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);

			ids.entreprise = entreprise.getNumero();
			ids.questionnaire = questionnaire.getId();
			return null;
		});

		// on lance le traitement -> le questionnaire SNC ne devrait pas être considéré
		final EchoirQuestionnairesSNCResults results = processor.run(RegDate.get(), null);
		assertNotNull(results);
		assertEquals(RegDate.get(), results.getDateTraitement());
		assertEquals(0, results.getTotal());
		assertEmpty(results.getErreurs());
		assertEmpty(results.getTraites());
	}

	/**
	 * Ce test vérifie qu'un questionnaire SNC rappelé mais avec un délai qui court toujours n'est pas considéré par le processeur d'échéances des questionnaires SNC.
	 */
	@Test
	public void testQuestionnaireRappeleMaisDelaiPasDepasse() throws Exception {

		class Ids {
			Long entreprise;
			Long questionnaire;
		}
		final Ids ids = new Ids();

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode = 2015;

		doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			// un questionnaire SNC rappelé mais avec un délai qui court toujours
			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
			addEtatDeclarationEmise(questionnaire, RegDate.get());
			addEtatDeclarationRappelee(questionnaire, RegDate.get(), RegDate.get());
			addDelaiDeclaration(questionnaire, RegDate.get(), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE);

			ids.entreprise = entreprise.getNumero();
			ids.questionnaire = questionnaire.getId();
			return null;
		});

		// on lance le traitement -> le questionnaire SNC ne devrait pas être considéré
		final EchoirQuestionnairesSNCResults results = processor.run(RegDate.get(), null);
		assertNotNull(results);
		assertEquals(RegDate.get(), results.getDateTraitement());
		assertEquals(0, results.getTotal());
		assertEmpty(results.getErreurs());
		assertEmpty(results.getTraites());
	}

	/**
	 * Ce test vérifie qu'un questionnaire SNC rappelé avec un délai dépassé est bien traité par le processeur d'échéances des questionnaires SNC.
	 */
	@Test
	public void testQuestionnaireRappeleEtDelaiDepasse() throws Exception {

		class Ids {
			long entreprise;
			long questionnaire;
		}
		final Ids ids = new Ids();

		final RegDate dateDebut = date(2008, 5, 1);
		final int periode = 2015;

		doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Ensemble pour aller plus loin");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			// un questionnaire SNC rappelé et avec un délai dépassé
			final PeriodeFiscale pf = addPeriodeFiscale(periode);
			final QuestionnaireSNC questionnaire = addQuestionnaireSNC(entreprise, pf);
			addEtatDeclarationEmise(questionnaire, RegDate.get(periode, 6, 1));
			addEtatDeclarationRappelee(questionnaire, RegDate.get(periode, 9, 1), RegDate.get(periode, 9, 1));
			addDelaiDeclaration(questionnaire, RegDate.get(periode, 6, 1), RegDate.get(periode, 5, 30), EtatDelaiDocumentFiscal.ACCORDE);

			ids.entreprise = entreprise.getNumero();
			ids.questionnaire = questionnaire.getId();
			return null;
		});

		// on lance le traitement -> le questionnaire SNC devrait être traité
		final RegDate dateTraitement = RegDate.get(periode + 1, 1, 1);
		final EchoirQuestionnairesSNCResults results = processor.run(dateTraitement, null);
		assertNotNull(results);
		assertEquals(dateTraitement, results.getDateTraitement());
		assertEquals(1, results.getTotal());
		assertEmpty(results.getErreurs());

		// le questionnaire doit être traité
		final List<EchoirQuestionnairesSNCResults.Traite> traites = results.getTraites();
		assertNotNull(traites);
		assertEquals(1, traites.size());

		final EchoirQuestionnairesSNCResults.Traite traite0 = traites.get(0);
		assertNotNull(traite0);
		assertEquals(ids.questionnaire, traite0.sncId);
		assertEquals(ids.entreprise, traite0.ctbId);
		assertEquals(RegDate.get(periode, 1, 1), traite0.dateDebut);
		assertEquals(RegDate.get(periode, 12, 31), traite0.dateFin);

		// le questionnaire est à l'état échu
		doInNewTransaction(status -> {
			final QuestionnaireSNC qsnc = hibernateTemplate.get(QuestionnaireSNC.class, ids.questionnaire);
			assertNotNull(qsnc);

			final EtatDeclaration dernierEtat = qsnc.getDernierEtatDeclaration();
			assertNotNull(dernierEtat);
			assertEquals(TypeEtatDocumentFiscal.ECHU, dernierEtat.getEtat());
			assertEquals(dateTraitement, dernierEtat.getDateObtention());
			assertEquals(dateTraitement, dernierEtat.getDateDebut());
			assertNull(dernierEtat.getDateFin());
			return null;
		});
	}
}