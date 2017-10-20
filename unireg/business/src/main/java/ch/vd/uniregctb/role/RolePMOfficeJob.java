package ch.vd.uniregctb.role;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.RolePMOfficeRapport;

public class RolePMOfficeJob extends RoleJob {

	private static final String JOB_NAME = "RolePMOfficeJob";

	public RolePMOfficeJob(int sortOrder, String description) {
		super(JOB_NAME, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getNbThreads(params);
		final int annee = getAnnee(params);

		final RolePMOfficeResults results = roleService.produireRolePMOffice(annee, nbThreads, getStatusManager());
		final RolePMOfficeRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);

		Audit.success("Le rôle PM " + annee + " de l'OIPM est terminé.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
