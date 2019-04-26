package ch.vd.unireg.rapport;

import java.util.List;
import java.util.Map;

import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.role.before2016.InfoCommunePM;
import ch.vd.unireg.role.before2016.InfoContribuablePM;
import ch.vd.unireg.role.before2016.ProduireRolesPMCommunesResults;

public class PdfRolesPMCommunesRapport extends PdfRolesCommunesRapport<ProduireRolesPMCommunesResults, InfoContribuablePM, InfoCommunePM> {

	public PdfRolesPMCommunesRapport(ServiceInfrastructureService infraService, AuditManager audit) {
		super(infraService, audit);
	}

	@Override
	protected TemporaryFile[] asCsvFiles(Map<Integer, String> nomsCommunes, InfoCommunePM infoCommune, StatusManager status) {

		final int noOfsCommune = infoCommune.getNoOfs();
		final List<InfoContribuablePM> infos = getListeTriee(infoCommune.getInfosContribuables());

		final String nomCommune = nomsCommunes.get(noOfsCommune);
		status.setMessage(String.format("Génération du rapport pour la commune de %s...", nomCommune));

		return traiteListeContribuablesPM(infos, nomsCommunes, infoContribuable -> noOfsCommune);
	}

	@Override
	protected Map<Integer, InfoCommunePM> getInfosCommunes(ProduireRolesPMCommunesResults results) {
		return results.getInfosCommunes();
	}
}
