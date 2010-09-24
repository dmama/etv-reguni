package ch.vd.uniregctb.civil.common;

import static junit.framework.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.common.AbstractBusinessTest;
import ch.vd.uniregctb.norentes.common.NorentesManager;
import ch.vd.uniregctb.norentes.common.NorentesScenario;

@ContextConfiguration(locations = {
		"classpath:unireg-norentes-main.xml",
		"classpath:unireg-norentes-scenarios.xml",
		"classpath:ut/unireg-norentesut-web.xml"
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
	@NotTransactional
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
