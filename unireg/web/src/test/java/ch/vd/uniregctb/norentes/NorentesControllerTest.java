package ch.vd.uniregctb.norentes;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebTest;

@ContextConfiguration(locations = {
	"classpath:unireg-norentes-main.xml",
	"classpath:unireg-norentes-scenarios.xml",
	"classpath:unireg-norentes-web.xml"
})
public class NorentesControllerTest extends WebTest {

	private NorentesController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(NorentesController.class, "norentesController");
	}

	@Test
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertNotNull("l'objet model retourné est null", model);
	}

}
