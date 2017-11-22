package ch.vd.uniregctb.registrefoncier.allegement;

import org.springframework.context.MessageSource;

import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalAvecSuiviView;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class DemandeDegrevementICIView extends AutreDocumentFiscalAvecSuiviView {

	private final int periodeFiscale;
	private final String codeControle;

	public DemandeDegrevementICIView(DemandeDegrevementICI doc, ServiceInfrastructureService infraService, MessageSource messageSource) {
		super(doc, infraService, messageSource,null, null);
		this.periodeFiscale = doc.getPeriodeFiscale();
		this.codeControle = doc.getCodeControle();
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public String getCodeControle() {
		return codeControle;
	}
}
