package ch.vd.uniregctb.tiers;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class TiersRapportControllerTest extends WebTest {

	private final static String CONTROLLER_NAME = "tiersRapportController";

	private TiersRapportController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(TiersRapportController.class, CONTROLLER_NAME);
	}

	/**
	 * Vérifie les données retournées pour l'édition d'un rapport d'appartenance ménage.
	 */
	@Test
	public void testShowForm() throws Exception {

		// Crée un ménage commun
		final PersonnePhysique jean = addNonHabitant("Jean", "Tiaget", date(1950, 1, 1), Sexe.MASCULIN);
		final PersonnePhysique jeanne = addNonHabitant("Jeanne", "Tiaget", date(1950, 1, 1), Sexe.FEMININ);
		final RegDate dateMariage = date(1975, 1, 1);
		addEnsembleTiersCouple(jean, jeanne, dateMariage, null);

		// Récupère le rapport d'appartenance ménage du principal
		final Set<RapportEntreTiers> rapports = jean.getRapportsSujet();
		assertNotNull(rapports);
		assertEquals(1, rapports.size());
		final AppartenanceMenage appartenance = (AppartenanceMenage) rapports.iterator().next();
		assertNotNull(appartenance);

		// Vérifie que les données exposées sont bien correctes
		request.setMethod("GET");
		request.addParameter("idRapport", String.valueOf(appartenance.getId()));
		request.addParameter("sens", "OBJET");
		final ModelAndView mav = controller.handleRequest(request, response);
		final RapportView view = (RapportView) mav.getModel().get(controller.getCommandName());
		assertNotNull(view);
		assertEquals(jean.getNumero(), view.getNumero());
		assertEquals(dateMariage, view.getRegDateDebut());
		assertNull(view.getRegDateFin());
		assertEquals("AppartenanceMenage", view.getNatureRapportEntreTiers());
		assertFalse(view.isAllowed());
		assertEquals(SensRapportEntreTiers.OBJET, view.getSensRapportEntreTiers());
	}

	/**
	 * Vérifie qu'il est possible de renseigner une date de fin sur un rapport de représentation conventionnelle.
	 */
	@Test
	public void testOnSubmit() throws Exception {

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

		// On simule l'affichage de la page d'édition de ce rapport
		{
			request.setMethod("GET");
			request.addParameter("idRapport", String.valueOf(ids.rapport));
			request.addParameter("sens", "OBJET");
			final ModelAndView mav = controller.handleRequest(request, response);
			final RapportView view = (RapportView) mav.getModel().get(controller.getCommandName());
			assertNotNull(view);
			assertEquals(ids.marcel, view.getNumero());
			assertEquals(date(2000, 1, 1), view.getRegDateDebut());
			assertNull(view.getRegDateFin());
			assertEquals("RepresentationConventionnelle", view.getNatureRapportEntreTiers());
			assertTrue(view.isAllowed());
			assertEquals(SensRapportEntreTiers.OBJET, view.getSensRapportEntreTiers());
		}

		// On simule l'édition de la date de fin du rapport
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				request.addParameter("idRapport", String.valueOf(ids.rapport));
				request.addParameter("sens", "OBJET");
				request.addParameter("dateFin", "29.11.2010"); // <-- date de fin renseignée
				request.setMethod("POST");
				controller.handleRequest(request, response);
				return null;
			}
		});

		// On vérifie que le rapport a bien été fermé
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RepresentationConventionnelle rapport = (RepresentationConventionnelle) hibernateTemplate.get(RepresentationConventionnelle.class, ids.rapport);
				assertNotNull(rapport);
				assertEquals(date(2010, 11, 29), rapport.getDateFin());
				return null;
			}
		});
	}

}
