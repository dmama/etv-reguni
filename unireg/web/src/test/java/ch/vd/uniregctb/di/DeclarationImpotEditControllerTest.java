package ch.vd.uniregctb.di;

import java.util.Map;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertNotNull;

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
				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);

			}
		});
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
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
}
