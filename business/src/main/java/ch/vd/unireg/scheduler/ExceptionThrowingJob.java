package ch.vd.unireg.scheduler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionThrowingJob extends JobDefinition {

	private final Logger LOGGER = LoggerFactory.getLogger(ExceptionThrowingJob.class);

	public static final String NAME = "ExceptionThrowing";

	public ExceptionThrowingJob(int sortOrder) {
		super(NAME, JobCategory.DEBUG, sortOrder, "Envoyer une exception (Pour le test)");
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
