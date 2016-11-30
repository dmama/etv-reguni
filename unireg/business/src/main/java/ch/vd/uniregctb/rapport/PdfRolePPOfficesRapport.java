package ch.vd.uniregctb.rapport;

import java.util.Collections;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.RolePPData;
import ch.vd.uniregctb.role.RolePPOfficesResults;

public class PdfRolePPOfficesRapport extends PdfRoleRapport<RolePPOfficesResults> {

	private static final RolePPDataFiller FILLER = new RolePPDataFiller();

	public PdfRolePPOfficesRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	protected void writePages(RolePPOfficesResults results, PdfWriter writer, StatusManager status) throws DocumentException {
		final List<OfficeImpot> offices = getSortedOfficeImpot(results.extraction.keySet(), infraService);
		for (OfficeImpot office : offices) {
			final List<RolePPData> data = results.extraction.getOrDefault(office.getNoColAdm(), Collections.emptyList());
			newPage(results, writer, data, office.getNomCourt(), String.format("OID%d", office.getNoColAdm()), status, FILLER);
		}
	}

	@Override
	protected void addAdditionalParameters(PdfTableSimple table, RolePPOfficesResults results) {
		super.addAdditionalParameters(table, results);
		table.addLigne("OID ciblé : ", results.oid != null ? String.valueOf(results.oid) : "Tous");
	}
}
