package ch.vd.unireg.rapport;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.role.RoleSNCResults;

public class PdfRoleSNCRapport extends PdfRoleRapport<RoleSNCResults> {

	private static final RolePMDataFiller FILLER = new RolePMDataFiller();

	public PdfRoleSNCRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	protected void writePages(RoleSNCResults results, PdfWriter writer, StatusManager status) throws DocumentException {
		newPage(results, writer, results.extraction, "OIPM", "OIPM", status, FILLER);
	}
}
