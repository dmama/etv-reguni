package ch.vd.unireg.rapport;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.role.RolePMOfficeResults;

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
