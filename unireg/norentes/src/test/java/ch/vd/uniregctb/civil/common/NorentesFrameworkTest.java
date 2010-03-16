package ch.vd.uniregctb.civil.common;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;

import ch.vd.uniregctb.norentes.common.NorentesFrameworkTestScenario;
import ch.vd.uniregctb.norentes.common.NorentesScenario;

public class NorentesFrameworkTest extends NorentesTest {

	NorentesScenario scenario = null;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		scenario = norentesManager.getScenario( getScenarioName());
	}

	@Override
	protected String getScenarioName() {
		return NorentesFrameworkTestScenario.NAME;
	}

	@Test
	public void testStaticInfos() throws Exception {

		assertEquals("NorentesFrameworkTestScenario", scenario.getName());
		assertTrue(scenario.getDescription().contains("framework"));
		assertEquals(3, scenario.getEtapeAttributes().size());
	}

	@Test
	@NotTransactional
	public void testStepBack() throws Exception {

		norentesManager.runToStep(scenario, 2);

		norentesManager.runToStep(scenario, 1);

		norentesManager.runToStep(scenario, 3);

		norentesManager.runToStep(scenario, 2);

		norentesManager.runToStep(scenario, 1);

	}

	@Test
	@NotTransactional
	public void testRunToLast() throws Exception {
		norentesManager.runToLast(scenario);
	}

	@Test
	@NotTransactional
	public void testRunToLastFrom() throws Exception {

		norentesManager.runToStep(scenario, 2);
		norentesManager.runToLast(scenario);
	}

}
