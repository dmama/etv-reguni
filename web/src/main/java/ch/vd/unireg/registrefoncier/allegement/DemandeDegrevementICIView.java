package ch.vd.unireg.registrefoncier.allegement;

import org.springframework.context.MessageSource;

import ch.vd.unireg.documentfiscal.AutreDocumentFiscalAvecSuiviView;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public class DemandeDegrevementICIView extends AutreDocumentFiscalAvecSuiviView {

	private final String codeControle;

	public DemandeDegrevementICIView(DemandeDegrevementICI doc, ServiceInfrastructureService infraService, MessageSource messageSource) {
		super(doc, infraService, messageSource,null, null);
		this.codeControle = doc.getCodeControle();
	}

	public DemandeDegrevementICIView(DemandeDegrevementICI doc, ServiceInfrastructureService infraService, MessageSource messageSource, String typeKey, String subtypeKey) {
		super(doc, infraService, messageSource,typeKey, subtypeKey);
		this.codeControle = doc.getCodeControle();
	}

	public String getCodeControle() {
		return codeControle;
	}
}
