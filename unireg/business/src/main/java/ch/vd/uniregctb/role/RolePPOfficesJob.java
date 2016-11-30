package ch.vd.uniregctb.role;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.RolePPOfficesRapport;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamOfficeImpot;

public class RolePPOfficesJob extends RoleJob {

	private static final String JOB_NAME = "RolePPOfficesJob";
	private static final String PARAM_OID = "OID";

	public RolePPOfficesJob(int sortOrder, String description) {
		super(JOB_NAME, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("OID ciblé (optionnelle)");
			param.setEnabled(true);
			param.setMandatory(false);
			param.setName(PARAM_OID);
			param.setType(new JobParamOfficeImpot());
			addParameterDefinition(param, null);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getNbThreads(params);
		final int annee = getAnnee(params);
		final Integer oid = getOptionalIntegerValue(params, PARAM_OID);

		final RolePPOfficesResults results = roleService.produireRolePPOffices(annee, nbThreads, oid, getStatusManager());
		final RolePPOfficesRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);

		if (oid != null) {
			Audit.success("Le rôle PP " + annee + " de l'OID " + oid + " est terminé.", rapport);
		}
		else {
			Audit.success("Le rôle PP " + annee + " des OID vaudois est terminé.", rapport);
		}
	}
}
