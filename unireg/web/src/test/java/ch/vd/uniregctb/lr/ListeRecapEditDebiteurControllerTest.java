package ch.vd.uniregctb.lr;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author xcifde
 */
public class ListeRecapEditDebiteurControllerTest extends AbstractLrControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "lrEditDebiteurController";

	private ListeRecapEditDebiteurController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(ListeRecapEditDebiteurController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {
		//
		//Debiteur Prestation Imposable 12500001
		//
		request.setMethod("GET");
		request.addParameter("numero", "12500001");
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
		//
		//Debiteur Prestation Imposable 12500001
		//
		request.setMethod("POST");
		request.addParameter("numero", "12500001");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}
}
