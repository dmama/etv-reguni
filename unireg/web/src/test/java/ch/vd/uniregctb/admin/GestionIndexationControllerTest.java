package ch.vd.uniregctb.admin;

import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebTest;

public class GestionIndexationControllerTest extends WebTest {

	private GestionIndexationController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(GestionIndexationController.class, "gestionIndexationController");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull("l'objet model retourné est null", model);
	}

}
