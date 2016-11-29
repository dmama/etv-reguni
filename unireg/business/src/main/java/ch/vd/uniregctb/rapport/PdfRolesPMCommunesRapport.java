package ch.vd.uniregctb.rapport;

import java.util.List;
import java.util.Map;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.before2016.InfoCommunePM;
import ch.vd.uniregctb.role.before2016.InfoContribuablePM;
import ch.vd.uniregctb.role.before2016.ProduireRolesPMCommunesResults;

public class PdfRolesPMCommunesRapport extends PdfRolesCommunesRapport<ProduireRolesPMCommunesResults, InfoContribuablePM, InfoCommunePM> {

	public PdfRolesPMCommunesRapport(ServiceInfrastructureService infraService) {
		super(infraService);
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
