package ch.vd.uniregctb.evenement.party;

import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v2.AperiodicTaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.v2.TaxLiabilityRequest;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;
import ch.vd.uniregctb.xml.DataHelper;

public class AperiodicTaxLiabilityRequestHandlerV2 extends TaxLiabilityRequestHandler {


	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/aperiodic-taxliab-request-2.xsd");
	}


	@Override
	public TaxliabilityControlResult runControl(TaxliabilityControlManager controlManager, TaxLiabilityRequest request) throws ControlRuleException {
		AperiodicTaxLiabilityRequest aperiodicRequest = (AperiodicTaxLiabilityRequest)request;
		final RegDate dateControle = DataHelper.xmlToCore(aperiodicRequest.getDate());
		final long tiersId = request.getPartyNumber();
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		TaxliabilityControlResult result =  controlManager.runControlOnDate(tiersId, dateControle, rechercheMenageCommun, rechercheParents);
		return result;
	}


}
