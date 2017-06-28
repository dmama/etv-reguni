package ch.vd.uniregctb.rapport;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.util.NestedServletException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"JavaDoc"})
public class RapportControllerTest extends WebTest {

	private RapportController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(RapportController.class, "rapportController");
	}

	/**
	 * Vérifie qu'il n'est pas possible d'éditer un rapport d'appartenance ménage
	 */
	@Test
	public void testEditAppartenanceMenage() throws Exception {

		class Ids {
			Long jean;
			Long jeanne;
			Long menage;
			Long rapport;
		}
		final Ids ids = new Ids();

		// Crée un ménage commun
		doInNewTransaction(status -> {
			final PersonnePhysique jean = addNonHabitant("Jean", "Tiaget", date(1950, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique jeanne = addNonHabitant("Jeanne", "Tiaget", date(1950, 1, 1), Sexe.FEMININ);
			final RegDate dateMariage = date(1975, 1, 1);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(jean, jeanne, dateMariage, null);

			// Récupère le rapport d'appartenance ménage du principal
			final Set<RapportEntreTiers> rapports = jean.getRapportsSujet();
			assertNotNull(rapports);
			assertEquals(1, rapports.size());
			final AppartenanceMenage appartenance = (AppartenanceMenage) rapports.iterator().next();
			assertNotNull(appartenance);

			ids.jean = jean.getId();
			ids.jeanne = jeanne.getId();
			ids.menage = couple.getMenage().getId();
			ids.rapport = appartenance.getId();
			return null;
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		try {
			final ResultActions res = m.perform(get("/rapport/edit.do").param("idRapport", String.valueOf(ids.rapport)).param("sens", "OBJET"));
			res.andExpect(status().isOk());
			fail();
		}
		catch (NestedServletException e) {
			final AccessDeniedException cause = (AccessDeniedException) e.getCause();
			assertEquals("Vous ne possédez pas le droit d'édition du rapport-entre-tiers entre le tiers n°" + ids.menage + " et le tiers n°" + ids.jean, cause.getMessage());
		}
	}

	/**
	 * Vérifie les données retournées pour l'édition d'un rapport de représentation.
	 */
	@Test
	public void testEditRepresentation() throws Exception {

		final RegDate dateDebutRapport = date(1990, 1, 1);

		class Ids {
			Long jean;
			Long jeanne;
			Long rapport;
		}
		final Ids ids = new Ids();

		// Crée un ménage commun et le rapport
		doInNewTransaction(status -> {
			final PersonnePhysique jean = addNonHabitant("Jean", "Tiaget", date(1950, 1, 1), Sexe.MASCULIN);
			addForPrincipal(jean, date(1970, 1, 1), MotifFor.MAJORITE, MockCommune.Aigle);
			final PersonnePhysique jeanne = addNonHabitant("Jeanne", "Tiaget", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(jeanne, date(1970, 1, 1), MotifFor.MAJORITE, MockCommune.Aigle);
			final RepresentationConventionnelle rapport = addRepresentationConventionnelle(jeanne, jean, dateDebutRapport, false);
			ids.jean = jean.getId();
			ids.jeanne = jeanne.getId();
			ids.rapport = rapport.getId();
			return null;
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final ResultActions res = m.perform(get("/rapport/edit.do").param("idRapport", String.valueOf(ids.rapport)).param("sens", "OBJET"));
		res.andExpect(status().isOk());

		// Vérifie que les données exposées sont bien correctes
		final MvcResult result = res.andReturn();
		assertNotNull(result);
		final Map<String, Object> model = result.getModelAndView().getModel();
		assertNotNull(model);

		// Vérifie que les données exposées sont bien correctes
		final RapportView view = (RapportView) model.get("rapportEditView");
		assertNotNull(view);
		assertEquals(ids.jean, view.getNumeroCourant());
		assertEquals(ids.jeanne, view.getNumero());
		assertEquals(dateDebutRapport, view.getDateDebut());
		assertNull(view.getDateFin());
		assertEquals("RepresentationConventionnelle", view.getNatureRapportEntreTiers());
		assertTrue(view.isAllowed());
		assertEquals(SensRapportEntreTiers.OBJET, view.getSensRapportEntreTiers());
	}

	/**
	 * Vérifie qu'il est possible de renseigner une date de fin sur un rapport de représentation conventionnelle.
	 */
	@Test
	public void testEditPost() throws Exception {

		class Ids {
			Long marcel;
			Long geraldine;
			Long rapport;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marcel = addNonHabitant("Marcel", "Ragnol", date(1932, 1, 1), Sexe.MASCULIN);
				ids.marcel = marcel.getId();
				final PersonnePhysique geraldine = addNonHabitant("Géraldine", "Massnacht", date(1982, 1, 1), Sexe.FEMININ);
				ids.geraldine = geraldine.getId();
				final RepresentationConventionnelle rapport = addRepresentationConventionnelle(marcel, geraldine, date(2000, 1, 1), false);
				ids.rapport = rapport.getId();
				return null;
			}
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		// On provoque l'édition de la date de fin du rapport
		final ResultActions res = m.perform(post("/rapport/edit.do")
				                                    .param("id", String.valueOf(ids.rapport))
				                                    .param("typeRapportEntreTiers", "REPRESENTATION")
				                                    .param("sensRapportEntreTiers", "OBJET")
				                                    .param("numeroCourant", String.valueOf(ids.marcel))
				                                    .param("numero", String.valueOf(ids.geraldine))
				                                    .param("dateFin", "29.11.2010")
				                                    .param("extensionExecutionForcee", "false"));
		res.andExpect(status().isMovedTemporarily());

		// On vérifie que le rapport a bien été fermé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RepresentationConventionnelle rapport = hibernateTemplate.get(RepresentationConventionnelle.class, ids.rapport);
				assertNotNull(rapport);
				assertEquals(date(2010, 11, 29), rapport.getDateFin());
				return null;
			}
		});
	}
}
