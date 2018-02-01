package ch.vd.unireg.entreprise.complexe;

import java.util.List;

import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FusionEntreprisesControllerTest extends WebTestSpring3 {

	public FusionEntreprisesControllerTest() {
		setWantIndexationTiers(true);
	}

	@Test
	public void testEntrepriseCandidateAbsorptionRadiee() throws Exception {

		final RegDate dateDebutAbsorbante = date(2003, 5, 12);
		final RegDate dateDebutCandidate = date(2010, 1, 31);
		final RegDate dateRadiationCandidate = date(2015, 7, 3);
		final RegDate dateContrat = date(2016, 1,1);
		final RegDate dateBilan = date(2016, 1,1);

		final class Ids {
			long pmAbsorbante;
			long pmAbsorbee;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise absorbante = addEntrepriseInconnueAuCivil();
			addFormeJuridique(absorbante, dateDebutAbsorbante, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(absorbante, dateDebutAbsorbante, null, "Absorba SA");
			addRegimeFiscalCH(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(absorbante, dateDebutAbsorbante, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aigle);
			addEtatEntreprise(absorbante, dateDebutAbsorbante, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			final Entreprise absorbee = addEntrepriseInconnueAuCivil();
			addFormeJuridique(absorbee, dateDebutCandidate, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(absorbee, dateDebutCandidate, null, "Disparata SA");
			addRegimeFiscalCH(absorbee, dateDebutCandidate, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(absorbee, dateDebutCandidate, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(absorbee, dateDebutCandidate, MotifFor.DEBUT_EXPLOITATION, dateRadiationCandidate, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne);
			addEtatEntreprise(absorbee, dateDebutCandidate, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(absorbee, dateRadiationCandidate, TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			final Ids pms = new Ids();
			pms.pmAbsorbante = absorbante.getNumero();
			pms.pmAbsorbee = absorbee.getNumero();
			return pms;
		});

		// indexation en cours...
		globalTiersIndexer.sync();

		// données dans la session après le choix des dates (on se place à ce niveau-là)
		session.setAttribute(FusionEntreprisesController.FUSION_NAME, new FusionEntreprisesSessionData(ids.pmAbsorbante, dateContrat, dateBilan));

		// maintenant, on va lancer une recherche sur les entreprises absorbée avec la bonne raison sociale
		// (on s'attend à ce que l'entreprise revienne mais ne soit pas sélectionnable)
		final TiersCriteriaView criteria = new TiersCriteriaView();
		criteria.setNomRaison("disparata");
		session.setAttribute(FusionEntreprisesController.CRITERIA_NAME_ABSORBEE, criteria);

		request.setRequestURI("/processuscomplexe/fusion/absorbees/list.do");
		request.addParameter("searched", "true");
		request.setMethod("GET");

		// appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// on vérifie que tout s'est déroulé sans accroc
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		// et on vérifie l'état de ce qui nous revient de la recherche
		//noinspection unchecked
		final List<SelectionEntrepriseView> candidates = (List<SelectionEntrepriseView>) mav.getModel().get("list");
		assertNotNull(candidates);
		assertEquals(1, candidates.size());

		final SelectionEntrepriseView viewCandidate = candidates.get(0);
		assertNotNull(viewCandidate);
		assertEquals((Long) ids.pmAbsorbee, viewCandidate.getNumero());
		assertFalse(viewCandidate.isSelectionnable());
		assertEquals("Entreprise radiée du RC avant les dates de bilan/contrat de fusion", viewCandidate.getExplicationNonSelectionnable());
	}

	@Test
	public void testEntrepriseCandidateAbsorptionNonRadieeADateFusion() throws Exception {

		final RegDate dateDebutAbsorbante = date(2003, 5, 12);
		final RegDate dateDebutCandidate = date(2010, 1, 31);
		final RegDate dateRadiationCandidate = date(2015, 7, 3);
		final RegDate dateContrat = date(2014, 1,1);                // = avant la radiation
		final RegDate dateBilan = date(2014, 1,1);

		final class Ids {
			long pmAbsorbante;
			long pmAbsorbee;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise absorbante = addEntrepriseInconnueAuCivil();
			addFormeJuridique(absorbante, dateDebutAbsorbante, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(absorbante, dateDebutAbsorbante, null, "Absorba SA");
			addRegimeFiscalCH(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(absorbante, dateDebutAbsorbante, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aigle);
			addEtatEntreprise(absorbante, dateDebutAbsorbante, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			final Entreprise absorbee = addEntrepriseInconnueAuCivil();
			addFormeJuridique(absorbee, dateDebutCandidate, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(absorbee, dateDebutCandidate, null, "Disparata SA");
			addRegimeFiscalCH(absorbee, dateDebutCandidate, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(absorbee, dateDebutCandidate, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(absorbee, dateDebutCandidate, MotifFor.DEBUT_EXPLOITATION, dateRadiationCandidate, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne);
			addEtatEntreprise(absorbee, dateDebutCandidate, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(absorbee, dateRadiationCandidate, TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			final Ids pms = new Ids();
			pms.pmAbsorbante = absorbante.getNumero();
			pms.pmAbsorbee = absorbee.getNumero();
			return pms;
		});

		// indexation en cours...
		globalTiersIndexer.sync();

		// données dans la session après le choix des dates (on se place à ce niveau-là)
		session.setAttribute(FusionEntreprisesController.FUSION_NAME, new FusionEntreprisesSessionData(ids.pmAbsorbante, dateContrat, dateBilan));

		// maintenant, on va lancer une recherche sur les entreprises absorbée avec la bonne raison sociale
		final TiersCriteriaView criteria = new TiersCriteriaView();
		criteria.setNomRaison("disparata");
		session.setAttribute(FusionEntreprisesController.CRITERIA_NAME_ABSORBEE, criteria);

		request.setRequestURI("/processuscomplexe/fusion/absorbees/list.do");
		request.addParameter("searched", "true");
		request.setMethod("GET");

		// appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// on vérifie que tout s'est déroulé sans accroc
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		// et on vérifie l'état de ce qui nous revient de la recherche
		//noinspection unchecked
		final List<SelectionEntrepriseView> candidates = (List<SelectionEntrepriseView>) mav.getModel().get("list");
		assertNotNull(candidates);
		assertEquals(1, candidates.size());

		final SelectionEntrepriseView viewCandidate = candidates.get(0);
		assertNotNull(viewCandidate);
		assertEquals((Long) ids.pmAbsorbee, viewCandidate.getNumero());
		assertTrue(viewCandidate.isSelectionnable());
		assertNull(viewCandidate.getExplicationNonSelectionnable());
	}

	@Test
	public void testEntrepriseCandidateAbsorptionRadieePuisReinscrite() throws Exception {

		final RegDate dateDebutAbsorbante = date(2003, 5, 12);
		final RegDate dateDebutCandidate = date(2010, 1, 31);
		final RegDate dateRadiationCandidate = date(2015, 7, 3);
		final RegDate dateReinscriptionCandidate = date(2015, 12, 3);
		final RegDate dateContrat = date(2016, 1,1);
		final RegDate dateBilan = date(2016, 1,1);

		final class Ids {
			long pmAbsorbante;
			long pmAbsorbee;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise absorbante = addEntrepriseInconnueAuCivil();
			addFormeJuridique(absorbante, dateDebutAbsorbante, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(absorbante, dateDebutAbsorbante, null, "Absorba SA");
			addRegimeFiscalCH(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(absorbante, dateDebutAbsorbante, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aigle);
			addEtatEntreprise(absorbante, dateDebutAbsorbante, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			final Entreprise absorbee = addEntrepriseInconnueAuCivil();
			addFormeJuridique(absorbee, dateDebutCandidate, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(absorbee, dateDebutCandidate, null, "Disparata SA");
			addRegimeFiscalCH(absorbee, dateDebutCandidate, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(absorbee, dateDebutCandidate, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(absorbee, dateDebutCandidate, MotifFor.DEBUT_EXPLOITATION, dateRadiationCandidate, MotifFor.DEPART_HS, MockCommune.Aubonne);
			addForPrincipal(absorbee, dateRadiationCandidate.getOneDayAfter(), MotifFor.DEPART_HS, dateReinscriptionCandidate.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Japon);
			addForPrincipal(absorbee, dateReinscriptionCandidate, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
			addEtatEntreprise(absorbee, dateDebutCandidate, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(absorbee, dateRadiationCandidate, TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(absorbee, dateReinscriptionCandidate, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			final Ids pms = new Ids();
			pms.pmAbsorbante = absorbante.getNumero();
			pms.pmAbsorbee = absorbee.getNumero();
			return pms;
		});

		// indexation en cours...
		globalTiersIndexer.sync();

		// données dans la session après le choix des dates (on se place à ce niveau-là)
		session.setAttribute(FusionEntreprisesController.FUSION_NAME, new FusionEntreprisesSessionData(ids.pmAbsorbante, dateContrat, dateBilan));

		// maintenant, on va lancer une recherche sur les entreprises absorbée avec la bonne raison sociale
		final TiersCriteriaView criteria = new TiersCriteriaView();
		criteria.setNomRaison("disparata");
		session.setAttribute(FusionEntreprisesController.CRITERIA_NAME_ABSORBEE, criteria);

		request.setRequestURI("/processuscomplexe/fusion/absorbees/list.do");
		request.addParameter("searched", "true");
		request.setMethod("GET");

		// appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// on vérifie que tout s'est déroulé sans accroc
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		// et on vérifie l'état de ce qui nous revient de la recherche
		//noinspection unchecked
		final List<SelectionEntrepriseView> candidates = (List<SelectionEntrepriseView>) mav.getModel().get("list");
		assertNotNull(candidates);
		assertEquals(1, candidates.size());

		final SelectionEntrepriseView viewCandidate = candidates.get(0);
		assertNotNull(viewCandidate);
		assertEquals((Long) ids.pmAbsorbee, viewCandidate.getNumero());
		assertTrue(viewCandidate.isSelectionnable());
		assertNull(viewCandidate.getExplicationNonSelectionnable());
	}

}
