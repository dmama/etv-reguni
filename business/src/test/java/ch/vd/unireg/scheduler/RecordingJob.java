package ch.vd.unireg.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Job qui enregistre ses exécutions, attend un petit moment et ne fait rien d'autre.
 */
public class RecordingJob extends JobDefinition {

	public static final List<Map<String, Object>> executions = Collections.synchronizedList(new ArrayList<>());

	public static final String NAME = "RecordingJob";
	public static final String DELAY = "delay";

	public RecordingJob(int sortOrder) {
		super(NAME, JobCategory.TEST, sortOrder, "Job qui enregistre ces exécutions et ne fait rien d'autre");

		final JobParam param = new JobParam();
		param.setDescription("Délai d'exécution");
		param.setName(DELAY);
		param.setMandatory(false);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, null);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		executions.add(params);

		Integer delay = getOptionalIntegerValue(params, DELAY);
		if (delay == null) {
			delay = 100;
		}
		Thread.sleep(delay);

	}
}
