package ch.vd.uniregctb.tiers;

import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebTest;

public class DeclarationImpotVisuControllerTest extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "diVisuController";

	private final static String DB_UNIT_FILE = "DeclarationImpotVisuControllerTest.xml";


	private DeclarationImpotVisuController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		controller = getBean(DeclarationImpotVisuController.class, CONTROLLER_NAME);

	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.setMethod("GET");
		request.addParameter("idDi", "210");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

}
