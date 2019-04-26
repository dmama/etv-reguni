package ch.vd.unireg.rapport;

import java.util.List;
import java.util.Map;

import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.role.before2016.InfoCommunePP;
import ch.vd.unireg.role.before2016.InfoContribuablePP;
import ch.vd.unireg.role.before2016.ProduireRolesPPCommunesResults;

public class PdfRolesPPCommunesRapport extends PdfRolesCommunesRapport<ProduireRolesPPCommunesResults, InfoContribuablePP, InfoCommunePP> {

	public PdfRolesPPCommunesRapport(ServiceInfrastructureService infraService, AuditManager audit) {
		super(infraService, audit);
	}

	@Override
	protected TemporaryFile[] asCsvFiles(Map<Integer, String> nomsCommunes, InfoCommunePP infoCommune, StatusManager status) {

		final int noOfsCommune = infoCommune.getNoOfs();
		final List<InfoContribuablePP> infos = getListeTriee(infoCommune.getInfosContribuables());

		final String nomCommune = nomsCommunes.get(noOfsCommune);
		status.setMessage(String.format("Génération du rapport pour la commune de %s...", nomCommune));

		return traiteListeContribuablesPP(infos, nomsCommunes, infoContribuable -> noOfsCommune);
	}

	@Override
	protected Map<Integer, InfoCommunePP> getInfosCommunes(ProduireRolesPPCommunesResults results) {
		return results.getInfosCommunes();
	}
}
