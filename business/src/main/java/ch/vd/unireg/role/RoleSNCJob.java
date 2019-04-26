package ch.vd.unireg.role;

import java.util.Map;

import ch.vd.unireg.document.RoleSNCRapport;

public class RoleSNCJob extends RoleJob {

	private static final String JOB_NAME = "RoleSNCJob";

	public RoleSNCJob(int sortOrder, String description) {
		super(JOB_NAME, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getNbThreads(params);
		final int annee = getAnnee(params);

		final RoleSNCResults results = roleService.produireRoleSNC(annee, nbThreads, getStatusManager());
		final RoleSNCRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);

		audit.success("Le rôle PM SNC " + annee + " de l'OIPM est terminé.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
