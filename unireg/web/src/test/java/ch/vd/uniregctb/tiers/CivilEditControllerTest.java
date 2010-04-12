package ch.vd.uniregctb.tiers;

import java.util.List;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class CivilEditControllerTest extends WebTest {

	private CivilEditController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(CivilEditController.class, "civilEditController");
	}

	/**
	 * [UNIREG-2233] Vérifie qu'il n'est pas possible de mettre un nom vide sur un non-habitant
	 */
	@Test
	public void testSubmitPrenomNomVides() throws Exception {

		final Long id = (Long) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique nh = addNonHabitant("", "temp", date(1967, 11, 1), Sexe.FEMININ);
				return nh.getNumero();
			}
		});

		// affiche la page de modification du tiers
		{
			request.setMethod("GET");
			request.addParameter("id", id.toString());

			final ModelAndView mav = controller.handleRequest(request, response);
			assertNotNull(mav);

			final TiersEditView view = (TiersEditView) mav.getModel().get("command");
			assertNotNull(view);
		}

		// essaie de modifier le nom par des espaces -> opération non-autorisée
		{
			request = new MockHttpServletRequest();
			request.setSession(session); // note: le form backing object est conservé dans la session
			request.addParameter("tiers.nom", "      ");
			request.setMethod("POST");

			final ModelAndView mav = controller.handleRequest(request, response);
			assertNotNull(mav);

			// vérification que l'erreur a bien été catchée et qu'on va afficher un gentil message à l'utilisateur.
			final BeanPropertyBindingResult exception = getBindingResult(mav);
			assertNotNull(exception);
			assertEquals(1, exception.getErrorCount());

			final List<?> errors = exception.getAllErrors();
			final FieldError error = (FieldError) errors.get(0);
			assertNotNull(error);
			assertEquals("tiers.nom", error.getField());
			assertEquals("error.tiers.nom.vide", error.getCode());
		}
	}
}
