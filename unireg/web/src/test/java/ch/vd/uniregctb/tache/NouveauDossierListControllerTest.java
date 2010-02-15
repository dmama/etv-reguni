package ch.vd.uniregctb.tache;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

public class NouveauDossierListControllerTest extends AbstractTacheControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "nouveauDossierListController";
	private NouveauDossierListController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(NouveauDossierListController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		request.setMethod("GET");
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
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}


}
