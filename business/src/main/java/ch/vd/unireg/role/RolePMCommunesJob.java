package ch.vd.unireg.role;

import java.util.Map;

import ch.vd.unireg.document.RolePMCommunesRapport;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamCommune;

public class RolePMCommunesJob extends RoleJob {

	private static final String JOB_NAME = "RolePMCommunesJob";
	private static final String PARAM_COMMUNE = "COMMUNE";

	public RolePMCommunesJob(int sortOrder, String description) {
		super(JOB_NAME, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Commune ciblée (optionnelle)");
			param.setEnabled(true);
			param.setMandatory(false);
			param.setName(PARAM_COMMUNE);
			param.setType(new JobParamCommune(JobParamCommune.TypeCommune.COMMUNE_VD));
			addParameterDefinition(param, null);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getNbThreads(params);
		final int annee = getAnnee(params);
		final Integer ofsCommune = getOptionalIntegerValue(params, PARAM_COMMUNE);

		final RolePMCommunesResults results = roleService.produireRolePMCommunes(annee, nbThreads, ofsCommune, getStatusManager());
		final RolePMCommunesRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);

		if (ofsCommune != null) {
			audit.success("Le rôle PM " + annee + " de la commune " + ofsCommune + " est terminé.", rapport);
		}
		else {
			audit.success("Le rôle PM " + annee + " des communes vaudoises est terminé.", rapport);
		}
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
