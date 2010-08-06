package ch.vd.uniregctb.di;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.EditiqueCommunicationException;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeDocument;

public class DeclarationImpotEditControllerTest extends AbstractDiControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "diEditController";
	private DeclarationImpotEditController controller;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		controller = getBean(DeclarationImpotEditController.class, CONTROLLER_NAME);
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu1 = addIndividu(320073L, RegDate.get(1960, 1, 1), "Totor", "Marcel", true);
				addAdresse(individu1, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);

			}
		});
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("action", DeclarationImpotEditController.ACTION_EDIT_DI);
		request.addParameter("id", idDI1.toString());
		final ModelAndView mav = controller.handleRequest(request, response);
		assertNotNull(mav);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmit() throws Exception {

		// affiche la page d'édition d'une DI (la session est chargée)
		{
			request.setMethod("GET");
			request.addParameter("action", DeclarationImpotEditController.ACTION_EDIT_DI);
			request.addParameter("id", idDI1.toString());
			final ModelAndView mav = controller.handleRequest(request, response);
			assertNotNull(mav);
		}

		// sauve la DI
		{
			request = new MockHttpServletRequest();
			request.setSession(session);
			request.setMethod("POST");
			request.addParameter("action", DeclarationImpotEditController.ACTION_EDIT_DI);
			request.addParameter(DeclarationImpotEditController.BUTTON_SAVE_DI, DeclarationImpotEditController.BUTTON_SAVE_DI);
			request.addParameter("id", idDI1.toString());
			final ModelAndView mav = controller.handleRequest(request, response);
			assertNotNull(mav);
			final Map<?, ?> model = mav.getModel();
			assertNotNull(model);
		}
	}

	/**
	 * Teste la création et l'impression d'une nouvelle déclaration d'impôt
	 */
	@Test
	public void testOnSubmitImpressionDI() throws Exception {

		// affiche la page de création d'une nouvelle DI
		{
			request.setMethod("GET");
			request.addParameter("action", DeclarationImpotEditController.ACTION_NEW_DI);
			request.addParameter("numero", "43308102");
			request.addParameter("debut", "20070101");
			request.addParameter("fin", "20071231");
			request.addParameter("typeDeclaration", TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL.toString());
			request.addParameter("delaiRetour", "60");
			request.addParameter("imprimable", "false");

			final ModelAndView mav = controller.handleRequest(request, response);
			assertNotNull(mav);

			final DeclarationImpotDetailView view = (DeclarationImpotDetailView)mav.getModel().get("command");
			assertNotNull(view);
			assertNull(view.getErrorMessage());
		}

		// sauvegarde et impression de la nouvelle DI
		{
			request = new MockHttpServletRequest();
			request.setSession(session); // note: le form backing object est conservé dans la session
			request.setMethod("POST");
			request.addParameter(AbstractSimpleFormController.PARAMETER_TARGET, DeclarationImpotEditController.TARGET_IMPRIMER_DI);

			// exécution de la requête
			try {
				controller.handleRequest(request, response);
			}
			catch (EditiqueCommunicationException e) {
				// ok
			}

			Tiers tiers = tiersDAO.get(new Long(43308102));
			Assert.assertEquals(1, tiers.getDeclarations().size());
		}
	}


	/**
	 * test de création de Di sans délai
	 */
	@Test
	public void testOnSubmitImpressionDiSansDelai() throws Exception {
		request.setMethod("POST");
		request.addParameter(DeclarationImpotEditController.TARGET_IMPRIMER_DI, "Imprimer");
		request.addParameter("numero", "43308102");
		request.addParameter("typeDeclarationImpot", TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL.toString());
		// exécution de la requête
		final ModelAndView mav = controller.handleRequest(request, response);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		Tiers tiers = tiersDAO.get(new Long(43308102));
		Assert.assertEquals(0, tiers.getDeclarations().size());
	}

}
