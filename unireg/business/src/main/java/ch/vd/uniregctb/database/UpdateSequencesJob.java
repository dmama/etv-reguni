package ch.vd.uniregctb.database;

import java.util.Map;

import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;

public class UpdateSequencesJob extends JobDefinition {

	// private final Logger LOGGER = LoggerFactory.getLogger(UpdateSequencesJob.class);

	public static final String NAME = "UpdateSequencesJob";

	public static final String UPDATE_HIBERNATE_SEQUENCE = "HIBERNATE";
	public static final String UPDATE_CAAC_SEQUENCE = "CAAC";
	public static final String UPDATE_PM_SEQUENCE = "PM";
	public static final String UPDATE_ETB_SEQUENCE = "ETB";
	public static final String UPDATE_DPI_SEQUENCE = "DPI";

	private DatabaseService service;

	public UpdateSequencesJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("MàJ de la séquence Hibernate");
			param.setName(UPDATE_HIBERNATE_SEQUENCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("MàJ de la séquence PM");
			param.setName(UPDATE_PM_SEQUENCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("MàJ de la séquence Col Adm. & Autres Comm.");
			param.setName(UPDATE_CAAC_SEQUENCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("MàJ de la séquence DPI");
			param.setName(UPDATE_DPI_SEQUENCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("MàJ de la séquence ETB");
			param.setName(UPDATE_ETB_SEQUENCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final boolean hibernate = getBooleanValue(params, UPDATE_HIBERNATE_SEQUENCE);
		final boolean caac = getBooleanValue(params, UPDATE_CAAC_SEQUENCE);
		final boolean pm = getBooleanValue(params, UPDATE_PM_SEQUENCE);
		final boolean dpi = getBooleanValue(params, UPDATE_DPI_SEQUENCE);
		final boolean etb = getBooleanValue(params, UPDATE_ETB_SEQUENCE);

		service.ensureSequencesUpToDate(hibernate, caac, dpi, pm, etb);
	}

	public void setService(DatabaseService service) {
		this.service = service;
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
