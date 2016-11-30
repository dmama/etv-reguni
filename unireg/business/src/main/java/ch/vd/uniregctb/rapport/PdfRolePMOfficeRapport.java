package ch.vd.uniregctb.rapport;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.RolePMOfficeResults;

public class PdfRolePMOfficeRapport extends PdfRoleRapport<RolePMOfficeResults> {

	private static final RolePMDataFiller FILLER = new RolePMDataFiller();

	public PdfRolePMOfficeRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	protected void writePages(RolePMOfficeResults results, PdfWriter writer, StatusManager status) throws DocumentException {
		newPage(results, writer, results.extraction, "OIPM", "OIPM", status, FILLER);
	}
}
