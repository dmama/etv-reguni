package ch.vd.uniregctb.tiers.rattrapage.origine;

import java.util.Map;

import ch.vd.uniregctb.document.RecuperationOriginesNonHabitantsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.TiersService;

public class RecuperationOriginesNonHabitantsJob extends JobDefinition {

	private static final String NAME = "RecuperationOriginesNonHabitantsJob";

	private static final String NB_THREADS = "NB_THREADS";
	private static final String DRY_RUN = "DRY_RUN";

	private TiersService tiersService;
	private RapportService rapportService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public RecuperationOriginesNonHabitantsJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Mode simulation");
			param.setName(DRY_RUN);
			param.setMandatory(false);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, false);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 8);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getIntegerValue(params, NB_THREADS);
		//noinspection ConstantConditions
		final boolean dryRun = getOptionnalBooleanValue(params, DRY_RUN, Boolean.FALSE);

		final RecuperationOriginesNonHabitantsResults results = tiersService.recupereOriginesNonHabitants(nbThreads, dryRun, getStatusManager());
		final RecuperationOriginesNonHabitantsRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
	}
}
