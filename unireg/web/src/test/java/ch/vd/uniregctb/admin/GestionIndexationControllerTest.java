package ch.vd.uniregctb.admin;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.NorentesWebTest;

public class GestionIndexationControllerTest extends NorentesWebTest {

	private GestionIndexationController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(GestionIndexationController.class, "gestionIndexationController");
	}

	@Test
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull("l'objet model retourné est null", model);
	}

}
