package ch.vd.uniregctb.rapport;

import java.util.Collections;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.RolePPCommunesResults;
import ch.vd.uniregctb.role.RolePPData;

public class PdfRolePPCommunesRapport extends PdfRoleRapport<RolePPCommunesResults> {

	private static final RolePPDataFiller FILLER = new RolePPDataFiller();

	public PdfRolePPCommunesRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	protected void writePages(RolePPCommunesResults results, PdfWriter writer, StatusManager status) throws DocumentException {
		final List<Commune> communes = getSortedCommunes(results.extraction.keySet(), RegDate.get(results.annee, 12, 31), infraService);
		for (Commune commune : communes) {
			final List<RolePPData> data = results.extraction.getOrDefault(commune.getNoOFS(), Collections.emptyList());
			newPage(results, writer, data, commune.getNomOfficiel(), String.valueOf(commune.getNoOFS()), status, FILLER);
		}
	}

	@Override
	protected void addAdditionalParameters(PdfTableSimple table, RolePPCommunesResults results) {
		super.addAdditionalParameters(table, results);
		table.addLigne("Commune ciblée : ", results.ofsCommune != null ? String.valueOf(results.ofsCommune) : "Toutes");
	}
}
