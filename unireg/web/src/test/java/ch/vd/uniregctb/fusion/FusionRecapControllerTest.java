package ch.vd.uniregctb.fusion;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

public class FusionRecapControllerTest extends AbstractFusionControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "fusionRecapController";
	private FusionRecapController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(FusionRecapController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("numeroNonHab", numeroNonHab.toString());
		request.addParameter("numeroHab", numeroHab.toString());
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
		request.addParameter("numeroNonHab", numeroNonHab.toString());
		request.addParameter("numeroHab", numeroHab.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

}
