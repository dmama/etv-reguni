package ch.vd.uniregctb.rt;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.rt.DebiteurListController;

public class DebiteurListControllerTest extends AbstractRapportPrestationControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "debiteurListController";
	private DebiteurListController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(DebiteurListController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("numeroSrc", numeroSrc.toString());
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
		request.addParameter("numeroSrc", numeroSrc.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}


}
