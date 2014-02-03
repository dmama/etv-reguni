package ch.vd.uniregctb.evenement.party;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.party.taxliab.periodic.v3.PeriodicTaxLiabilityRequest;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;
import ch.vd.uniregctb.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.uniregctb.tiers.Tiers;

public class PeriodicTaxLiabilityRequestHandlerV3 extends TaxLiabilityRequestHandlerV3<PeriodicTaxLiabilityRequest> {

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/periodic-taxliab-request-3.xsd");
	}

	@Override
	public TaxLiabilityControlResult doControl(PeriodicTaxLiabilityRequest request, @NotNull Tiers tiers) throws ControlRuleException {
		final int periode = request.getFiscalPeriod();
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		return getTaxliabilityControlService().doControlOnPeriod(tiers, periode, rechercheMenageCommun, rechercheParents, true);
	}
}
