package ch.vd.unireg.role;

import java.util.Map;

import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.document.RolePPCommunesRapport;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamCommune;

public class RolePPCommunesJob extends RoleJob {

	private static final String JOB_NAME = "RolePPCommunesJob";
	private static final String PARAM_COMMUNE = "COMMUNE";

	public RolePPCommunesJob(int sortOrder, String description) {
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

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(annee, nbThreads, ofsCommune, getStatusManager());
		final RolePPCommunesRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);

		if (ofsCommune != null) {
			Audit.success("Le rôle PP " + annee + " de la commune " + ofsCommune + " est terminé.", rapport);
		}
		else {
			Audit.success("Le rôle PP " + annee + " des communes vaudoises est terminé.", rapport);
		}
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
