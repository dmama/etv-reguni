package ch.vd.uniregctb.civil.common;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.norentes.common.NorentesFrameworkTestScenario;
import ch.vd.uniregctb.norentes.common.NorentesScenario;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

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
	@Transactional(rollbackFor = Throwable.class)
	public void testStaticInfos() throws Exception {

		assertEquals("NorentesFrameworkTestScenario", scenario.getName());
		assertTrue(scenario.getDescription().contains("framework"));
		assertEquals(3, scenario.getEtapeAttributes().size());
	}

	@Test
	public void testStepBack() throws Exception {

		norentesManager.runToStep(scenario, 2);

		norentesManager.runToStep(scenario, 1);

		norentesManager.runToStep(scenario, 3);

		norentesManager.runToStep(scenario, 2);

		norentesManager.runToStep(scenario, 1);

	}

	@Test
	public void testRunToLast() throws Exception {
		norentesManager.runToLast(scenario);
	}

	@Test
	public void testRunToLastFrom() throws Exception {

		norentesManager.runToStep(scenario, 2);
		norentesManager.runToLast(scenario);
	}

}
