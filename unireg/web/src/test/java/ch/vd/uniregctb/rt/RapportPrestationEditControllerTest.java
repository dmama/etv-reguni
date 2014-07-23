package ch.vd.uniregctb.rt;

import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

public class RapportPrestationEditControllerTest extends AbstractRapportPrestationControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "rapportPrestationEditController";
	private RapportPrestationEditController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(RapportPrestationEditController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("numeroSrc", numeroSrc.toString());
		request.addParameter("numeroDpi", numeroDpi.toString());
		request.addParameter("provenance", PROVENANCE_SOURCIER);
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
		request.addParameter("numeroDpi", numeroDpi.toString());
		request.addParameter("provenance", "sourcier");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}


}
