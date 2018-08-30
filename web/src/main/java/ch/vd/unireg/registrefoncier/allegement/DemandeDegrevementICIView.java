package ch.vd.unireg.registrefoncier.allegement;

import ch.vd.unireg.documentfiscal.AutreDocumentFiscalAvecSuiviView;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;

public class DemandeDegrevementICIView extends AutreDocumentFiscalAvecSuiviView {

	private final String codeControle;

	public DemandeDegrevementICIView(DemandeDegrevementICI doc, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		super(doc, infraService, messageHelper, null, null);
		this.codeControle = doc.getCodeControle();
	}

	public DemandeDegrevementICIView(DemandeDegrevementICI doc, ServiceInfrastructureService infraService, MessageHelper messageHelper, String typeKey, String subtypeKey) {
		super(doc, infraService, messageHelper, typeKey, subtypeKey);
		this.codeControle = doc.getCodeControle();
	}

	public String getCodeControle() {
		return codeControle;
	}
}
