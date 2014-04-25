package ch.vd.uniregctb.tiers.rattrapage.nomsparents;

import java.util.Map;

import ch.vd.uniregctb.document.RecuperationNomsParentsAnciensHabitantsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.TiersService;

public class RecuperationNomsParentsAnciensHabitantsJob extends JobDefinition {

	private static final String NAME = "RecuperationNomsParentsAnciensHabitantsJob";
	private static final String CATEGORIE = "Database";

	private static final String NB_THREADS = "NB_THREADS";
	private static final String FORCE = "FORCE";

	private TiersService tiersService;
	private RapportService rapportService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public RecuperationNomsParentsAnciensHabitantsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Ecrasement des anciennes valeurs");
			param.setName(FORCE);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
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
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final boolean force = getBooleanValue(params, FORCE);

		final RecuperationNomsParentsAnciensHabitantsResults results = tiersService.recupereNomsParentsSurAnciensHabitants(nbThreads, force, getStatusManager());
		final RecuperationNomsParentsAnciensHabitantsRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
	}
}
