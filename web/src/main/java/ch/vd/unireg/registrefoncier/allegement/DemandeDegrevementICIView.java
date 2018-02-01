package ch.vd.uniregctb.registrefoncier.allegement;

import org.springframework.context.MessageSource;

import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalAvecSuiviView;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

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
