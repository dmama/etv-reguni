package ch.vd.unireg.norentes.common;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.unireg.common.AbstractBusinessTest;

import static org.junit.Assert.assertNotNull;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-norentes-main.xml",
		"classpath:ut/unireg-norentes-scenarios.xml"
})
public abstract class NorentesTest extends AbstractBusinessTest {

	//private static final Logger LOGGER = LoggerFactory.getLogger(NorentesTest.class);

	protected NorentesManager norentesManager;

	protected NorentesTest() {
		setWantCollectivitesAdministratives(false);     // elles sont créées dans les scénarios
	}

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
	protected void removeTiersIndexData() {
	}

	@Override
	protected void removeMessageIdentificationIndexData() {
	}

	@Override
	protected void indexTiersData() {
	}

	@Override
	protected void indexMessagesIdentificationData() {
	}

	@Override
	protected void truncateDatabase() throws Exception {
	}

}
