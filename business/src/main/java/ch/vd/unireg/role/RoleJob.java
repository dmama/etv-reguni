package ch.vd.unireg.role;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

public abstract class RoleJob extends JobDefinition {

	private static final JobCategory JOB_CATEGORY = JobCategory.STATS;

	private static final String PARAM_ANNEE = "ANNEE";
	private static final String PARAM_NB_THREADS = "NB_THREADS";

	protected RoleService roleService;
	protected RapportService rapportService;

	public RoleJob(String name, int sortOrder, String description) {
		super(name, JOB_CATEGORY, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Année de référence");
			param.setEnabled(true);
			param.setMandatory(true);
			param.setName(PARAM_ANNEE);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, RegDate.get().year() - 1);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setEnabled(true);
			param.setMandatory(true);
			param.setName(PARAM_NB_THREADS);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
	}

	public void setRoleService(RoleService roleService) {
		this.roleService = roleService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	protected int getAnnee(Map<String, Object> params) {
		return getPositiveIntegerValue(params, PARAM_ANNEE);
	}

	protected int getNbThreads(Map<String, Object> params) {
		return getPositiveIntegerValue(params, PARAM_NB_THREADS);
	}
}
