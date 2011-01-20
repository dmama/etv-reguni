package ch.vd.uniregctb.database;

import java.util.Map;

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

	private DatabaseService service;

	public UpdateSequencesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("MàJ de la séquence Hibernate");
		param.setName(UPDATE_HIBERNATE_SEQUENCE);
		param.setMandatory(true);
		param.setType(new JobParamBoolean());
		addParameterDefinition(param, Boolean.TRUE);

		final JobParam param2 = new JobParam();
		param2.setDescription("MàJ de la séquence PM");
		param2.setName(UPDATE_PM_SEQUENCE);
		param2.setMandatory(true);
		param2.setType(new JobParamBoolean());
		addParameterDefinition(param2, Boolean.TRUE);

		final JobParam param3 = new JobParam();
		param3.setDescription("MàJ de la séquence DPI");
		param3.setName(UPDATE_DPI_SEQUENCE);
		param3.setMandatory(true);
		param3.setType(new JobParamBoolean());
		addParameterDefinition(param3, Boolean.TRUE);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final boolean hibernate = getBooleanValue(params, UPDATE_HIBERNATE_SEQUENCE);
		final boolean pm = getBooleanValue(params, UPDATE_PM_SEQUENCE);
		final boolean dpi = getBooleanValue(params, UPDATE_DPI_SEQUENCE);

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
