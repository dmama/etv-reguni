package ch.vd.uniregctb.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class LoggingJob extends JobDefinition {

	private final Logger LOGGER = Logger.getLogger(LoggingJob.class);

	public static final String NAME = "Logging";
	private static final String CATEGORIE = "Debug";

	public static final String I_DELAY = "delay";

	private static List<JobParam> params ;

	static {
		params = new ArrayList<JobParam>() ;
		JobParam param = new JobParam();
		param.setDescription("Délai");
		param.setName(I_DELAY);
		param.setMandatory(false);
		param.setType(new JobParamInteger());
		params.add(param);
	}

	public LoggingJob(int sortOrder) {
		super(NAME, CATEGORIE, sortOrder, "Logger des lignes dans le LOGGER (pour le test)", params);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		LOGGER.debug("LoggingJob started...");

		Integer delay = 1000; // 1 seconde
		if (params != null) {
			delay = (Integer)params.get(I_DELAY);
		}

		for (int i=0;i<10;i++) {
			setPercentDone(i*10, 100);
			Thread.sleep(delay);
			if (isInterrupted()) {
				LOGGER.debug("LoggingJob interrupted!");
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
