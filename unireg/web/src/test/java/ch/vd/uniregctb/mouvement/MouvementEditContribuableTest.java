package ch.vd.uniregctb.mouvement;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

public class MouvementEditContribuableTest extends AbstractMouvementControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "mouvementEditController";
	private MouvementEditController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(MouvementEditController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("numero", numero.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

}
