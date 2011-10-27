package ch.vd.uniregctb.couple;

import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

public class PersonnePhysiqueListControllerTest extends AbstractCoupleControllerTest {

	private PersonnePhysiqueListController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(PersonnePhysiqueListController.class, "personnePhysiqueListController");
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void showForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("numeroPP1", numeroPP1.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void onSubmit() throws Exception {

		request.setMethod("POST");
		request.addParameter("numeroPP1", numeroPP1.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

}
