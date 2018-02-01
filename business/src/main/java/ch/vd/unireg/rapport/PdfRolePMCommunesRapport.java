package ch.vd.unireg.rapport;

import java.util.Collections;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.role.RolePMCommunesResults;
import ch.vd.unireg.role.RolePMData;

public class PdfRolePMCommunesRapport extends PdfRoleRapport<RolePMCommunesResults> {

	private static final RolePMDataFiller FILLER = new RolePMDataFiller();

	public PdfRolePMCommunesRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	protected void writePages(RolePMCommunesResults results, PdfWriter writer, StatusManager status) throws DocumentException {
		final List<Commune> communes = getSortedCommunes(results.extraction.keySet(), RegDate.get(results.annee, 12, 31), infraService);
		for (Commune commune : communes) {
			final List<RolePMData> data = results.extraction.getOrDefault(commune.getNoOFS(), Collections.emptyList());
			newPage(results, writer, data, commune.getNomOfficiel(), String.valueOf(commune.getNoOFS()), status, FILLER);
		}
	}

	@Override
	protected void addAdditionalParameters(PdfTableSimple table, RolePMCommunesResults results) {
		super.addAdditionalParameters(table, results);
		table.addLigne("Commune ciblée : ", results.ofsCommune != null ? String.valueOf(results.ofsCommune) : "Toutes");
	}
}
