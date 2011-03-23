package ch.vd.uniregctb.scheduler;

import java.util.Map;

import org.apache.log4j.Logger;

public class ExceptionThrowingJob extends JobDefinition {

	private final Logger LOGGER = Logger.getLogger(ExceptionThrowingJob.class);

	public static final String NAME = "ExceptionThrowing";
	private static final String CATEGORIE = "Debug";

	public ExceptionThrowingJob(int sortOrder) {
		super(NAME, CATEGORIE, sortOrder, "Envoyer une exception (Pour le test)");
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		Thread.sleep(200);
		LOGGER.info("Throwing exception!");
		setRunningMessage("Exception de test lancée...");
		throw new IllegalArgumentException("Exception de test");
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
