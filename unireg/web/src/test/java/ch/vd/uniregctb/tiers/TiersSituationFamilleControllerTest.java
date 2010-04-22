package ch.vd.uniregctb.tiers;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.manager.SituationFamilleManager;

/**
 * Test le controller TiersSituationFamilleController
 *
 * @author xcifde
 *
 */
public class TiersSituationFamilleControllerTest extends WebTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "tiersSituationFamilleController";

	private final static String DB_UNIT_FILE = "TiersSituationFamilleControllerTest.xml";

	private SituationFamilleManager situationFamilleManager;

	private TiersSituationFamilleController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		controller = getBean(TiersSituationFamilleController.class, CONTROLLER_NAME);

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.setMethod("GET");
		request.addParameter("numero", "86006202");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOnSubmit() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		request.addParameter("numero", "86006202");
		request.addParameter("dateDebut", "08.08.2008");
		request.addParameter("etatCivil", "MARIE");
		request.addParameter("nombreEnfants", "4");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);

		/*
		 * TODO (FDE) enrichir test ?
		 */
	}

	public SituationFamilleManager getSituationFamilleManager() {
		return situationFamilleManager;
	}

	public void setSituationFamilleManager(SituationFamilleManager situationFamilleManager) {
		this.situationFamilleManager = situationFamilleManager;
	}

}


