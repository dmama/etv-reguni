package ch.vd.uniregctb.rapport;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

public class RapportEditControllerTest extends AbstractRapportControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "rapportEditController";
	private RapportEditController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(RapportEditController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("numeroTiers", numeroTiers.toString());
		request.addParameter("numeroTiersLie", numeroTiersLie.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmit() throws Exception {

		request.setMethod("POST");
		request.addParameter("numeroTiers", numeroTiers.toString());
		request.addParameter("numeroTiersLie", numeroTiersLie.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}


}
