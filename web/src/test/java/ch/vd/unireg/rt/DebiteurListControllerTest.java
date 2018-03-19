package ch.vd.unireg.rt;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

public class DebiteurListControllerTest extends AbstractRapportPrestationControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private static final String CONTROLLER_NAME = "debiteurListController";
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
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmit() throws Exception {

		request.setMethod("POST");
		request.addParameter("numeroSrc", numeroSrc.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}


}