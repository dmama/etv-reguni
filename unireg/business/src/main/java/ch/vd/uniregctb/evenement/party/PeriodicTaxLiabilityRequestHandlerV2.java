package ch.vd.uniregctb.evenement.party;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.party.taxliab.periodic.v2.PeriodicTaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.v2.TaxLiabilityRequest;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;

public class PeriodicTaxLiabilityRequestHandlerV2 extends TaxLiabilityRequestHandler {


	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/periodic-taxliab-request-2.xsd");
	}


	@Override
	public TaxliabilityControlResult runControl(TaxliabilityControlManager controlManager, TaxLiabilityRequest request) throws ControlRuleException {

		PeriodicTaxLiabilityRequest periodicRequest = (PeriodicTaxLiabilityRequest) request;
		final Integer periode = periodicRequest.getFiscalPeriod();
		final long tiersId = request.getPartyNumber();
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		TaxliabilityControlResult result = controlManager.runControlOnPeriode(tiersId, periode, rechercheMenageCommun, rechercheParents);
		return result;
	}
}
