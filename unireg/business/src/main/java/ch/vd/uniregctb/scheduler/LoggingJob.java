package ch.vd.uniregctb.scheduler;

import java.util.Map;

import org.apache.log4j.Logger;

public class LoggingJob extends JobDefinition {

	private final Logger LOGGER = Logger.getLogger(LoggingJob.class);

	public static final String NAME = "Logging";
	private static final String CATEGORIE = "Debug";

	public static final String I_DELAY = "delay";
	public static final String I_INT_DELAY = "interruption_delay";

	public LoggingJob(int sortOrder) {
		super(NAME, CATEGORIE, sortOrder, "Logger des lignes dans le LOGGER (pour le test)");

		{
			final JobParam param = new JobParam();
			param.setDescription("Délai d'exécution");
			param.setName(I_DELAY);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Délai d'interruption");
			param.setName(I_INT_DELAY);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		LOGGER.debug("LoggingJob started...");

		Integer delay = getOptionalIntegerValue(params, I_DELAY);
		if (delay == null) {
			delay = 100; // 0.1 seconde
		}

		Integer intdelay = getOptionalIntegerValue(params, I_INT_DELAY);
		if (intdelay == null) {
			intdelay = 0; // 0 seconde
		}

		for (int i = 0; i < 10; i++) {
			setPercentDone(i * 10, 100);
			Thread.sleep(delay);
			if (isInterrupted()) {
				LOGGER.debug("LoggingJob interrupted!");
				Thread.sleep(intdelay);
				return;
			}
		}

		setPercentDone(100, 100);
		LOGGER.debug("LoggingJob done.");
	}

	protected void setPercentDone(int done, int whole) {
		LOGGER.debug("LogginJob: " + done + " on " + whole + "%");
		setRunningMessage(done + " on " + whole + " (" + (done / (float) whole * 100) + "%) done...");
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
