package ch.vd.uniregctb.rapport;

import java.util.List;
import java.util.Map;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.InfoCommunePM;
import ch.vd.uniregctb.role.InfoContribuable;
import ch.vd.uniregctb.role.InfoContribuablePM;
import ch.vd.uniregctb.role.ProduireRolesPMCommunesResults;

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

		return traiteListeContribuablesPM(infos, nomsCommunes, new AccesCommune() {
			@Override
			public int getNoOfsCommune(InfoContribuable infoContribuable) {
				return noOfsCommune;
			}
		});
	}

	@Override
	protected Map<Integer, InfoCommunePM> getInfosCommunes(ProduireRolesPMCommunesResults results) {
		return results.getInfosCommunes();
	}
}