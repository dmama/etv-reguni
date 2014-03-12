package ch.vd.uniregctb.norentes.common;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.common.AbstractBusinessTest;

import static junit.framework.Assert.assertNotNull;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-norentes-main.xml",
		"classpath:ut/unireg-norentes-scenarios.xml"
})
public abstract class NorentesTest extends AbstractBusinessTest {

	//private static final Logger LOGGER = Logger.getLogger(NorentesTest.class);

	protected NorentesManager norentesManager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		norentesManager = getBean(NorentesManager.class, "norentesManager");
		assertNotNull(norentesManager);
	}

	@Override
	public void onTearDown() throws Exception {

		norentesManager.closeCurrentScenario();
		super.onTearDown();
	}

	protected abstract String getScenarioName();

	@Test
	public void testScenario() throws Exception {
		String name = getScenarioName();
		if (name != null) {
			runScenarioWithChecks(name);
		}
	}

	public void runScenarioWithChecks(String name) throws Exception {
		NorentesScenario scenario = norentesManager.getScenario(name);
		assertNotNull("Le scénario n'est pas connu (est-il enregistré dans le fichier unireg-norentes-scenarios.xml ?)", scenario);
		norentesManager.closeCurrentScenario();
		norentesManager.runToLast(scenario);
	}

	@Override
	protected void removeIndexData() throws Exception {
	}

	@Override
	protected void indexData() throws Exception {
	}

	@Override
	protected void truncateDatabase() throws Exception {
	}

}
