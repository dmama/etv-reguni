package ch.vd.unireg.rapport;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
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
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.rapport.view.RapportView;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Curatelle;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MockTiersService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RepresentationConventionnelle;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.Tutelle;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;

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

	@Test
	public void testSortRapportsByAutoriteTutelaire() {
		final ParamPagination pagination = new ParamPagination(0, 20, "autoriteTutelaire", true);
		List<RapportEntreTiers> rapports = new ArrayList<RapportEntreTiers>();
		// Curatelle
		CollectiviteAdministrative autoriteTutelaireCuratelle = new CollectiviteAdministrative(1L, 1, 1, 1);
		Curatelle curatelle= new Curatelle();
		curatelle.setId(111L);
		curatelle.setAutoriteTutelaireId(autoriteTutelaireCuratelle.getId());
		rapports.add(curatelle);
		// Appartenance ménage
		AppartenanceMenage appartenanceMenage = new AppartenanceMenage();
		appartenanceMenage.setId(222L);
		rapports.add(appartenanceMenage);
		// Tutelle
		CollectiviteAdministrative autoriteTutelaireTutelle = new CollectiviteAdministrative(2L, 2, 2, 2);
		Tutelle tutelle = new Tutelle();
		tutelle.setId(333L);
		tutelle.setAutoriteTutelaire(autoriteTutelaireTutelle);
		rapports.add(tutelle);


		TiersService mockTiersService = new MockTiersService() {

			@Override
			public Tiers getTiers(long idTiers){
				String value = Long.toString(idTiers);
				switch (value) {
					case "1": return autoriteTutelaireCuratelle;
					case "2": return autoriteTutelaireTutelle;
				}
				return null;
			}

			@Override
			public String getNomCollectiviteAdministrative(int numeroCollectivite) {
				String value = Integer.toString(numeroCollectivite);
				switch(value) {
					case "1" : return "abz";
					case "2" : return "abc";
				}
				return "inconnu";
			}
		};

		RapportController.sortRapportsByAutoriteTutelaire(pagination, rapports, mockTiersService);

		assertEquals(3, rapports.size());
		assertEquals(appartenanceMenage, rapports.get(0));
		assertEquals(tutelle, rapports.get(1));
		assertEquals(curatelle, rapports.get(2));
	}

}
