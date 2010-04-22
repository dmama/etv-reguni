package ch.vd.uniregctb.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;

public class UpdateSequencesJob extends JobDefinition {

	// private final Logger LOGGER = Logger.getLogger(UpdateSequencesJob.class);

	public static final String NAME = "UpdateSequencesJob";
	private static final String CATEGORIE = "Database";

	public static final String UPDATE_HIBERNATE_SEQUENCE = "HIBERNATE";
	public static final String UPDATE_PM_SEQUENCE = "PM";
	public static final String UPDATE_DPI_SEQUENCE = "DPI";

	private static final List<JobParam> params;
	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("MàJ de la séquence Hibernate");
			param.setName(UPDATE_HIBERNATE_SEQUENCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			params.add(param);

			JobParam param2 = new JobParam();
			param2.setDescription("MàJ de la séquence PM");
			param2.setName(UPDATE_PM_SEQUENCE);
			param2.setMandatory(true);
			param2.setType(new JobParamBoolean());
			params.add(param2);

			JobParam param3 = new JobParam();
			param3.setDescription("MàJ de la séquence DPI");
			param3.setName(UPDATE_DPI_SEQUENCE);
			param3.setMandatory(true);
			param3.setType(new JobParamBoolean());
			params.add(param3);
		}

		defaultParams = new HashMap<String, Object>();
		{
			defaultParams.put(UPDATE_HIBERNATE_SEQUENCE, Boolean.TRUE);
			defaultParams.put(UPDATE_PM_SEQUENCE, Boolean.TRUE);
			defaultParams.put(UPDATE_DPI_SEQUENCE, Boolean.TRUE);
		}
	}

	private DatabaseService service;

	public UpdateSequencesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final Boolean hibernate = (Boolean) params.get(UPDATE_HIBERNATE_SEQUENCE);
		final Boolean pm = (Boolean) params.get(UPDATE_PM_SEQUENCE);
		final Boolean dpi = (Boolean) params.get(UPDATE_DPI_SEQUENCE);

		service.ensureSequencesUpToDate(hibernate, pm, dpi);
	}

	public void setService(DatabaseService service) {
		this.service = service;
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
