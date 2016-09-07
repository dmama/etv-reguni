package ch.vd.uniregctb.rapport;

import java.util.List;
import java.util.Map;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.InfoCommunePP;
import ch.vd.uniregctb.role.InfoContribuable;
import ch.vd.uniregctb.role.InfoContribuablePP;
import ch.vd.uniregctb.role.ProduireRolesPPCommunesResults;

public class PdfRolesPPCommunesRapport extends PdfRolesCommunesRapport<ProduireRolesPPCommunesResults, InfoContribuablePP, InfoCommunePP> {

	public PdfRolesPPCommunesRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	protected TemporaryFile[] asCsvFiles(Map<Integer, String> nomsCommunes, InfoCommunePP infoCommune, StatusManager status) {

		final int noOfsCommune = infoCommune.getNoOfs();
		final List<InfoContribuablePP> infos = getListeTriee(infoCommune.getInfosContribuables());

		final String nomCommune = nomsCommunes.get(noOfsCommune);
		status.setMessage(String.format("Génération du rapport pour la commune de %s...", nomCommune));

		return traiteListeContribuablesPP(infos, nomsCommunes, new AccesCommune() {
			@Override
			public int getNoOfsCommune(InfoContribuable infoContribuable) {
				return noOfsCommune;
			}
		});
	}

	@Override
	protected Map<Integer, InfoCommunePP> getInfosCommunes(ProduireRolesPPCommunesResults results) {
		return results.getInfosCommunes();
	}
}
