package ch.vd.uniregctb.lr;

import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;


/**
 * Test case du controlleur spring du meme nom.
 *
 * @author xcifde
 */
public class ListeRecapEditControllerTest extends AbstractLrControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "lrEditController";
	private ListeRecapEditController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(ListeRecapEditController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("id", idLR1.toString());
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
		request.addParameter(ListeRecapEditController.BUTTON_SAUVER, ListeRecapEditController.BUTTON_SAUVER);
		request.addParameter("id", idLR1.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

}
