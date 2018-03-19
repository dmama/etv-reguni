package ch.vd.unireg.evenement.party;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.party.taxliab.periodic.v2.PeriodicTaxLiabilityRequest;
import ch.vd.unireg.evenement.party.control.ControlRuleException;
import ch.vd.unireg.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.Tiers;

public class PeriodicTaxLiabilityRequestHandlerV2 extends TaxLiabilityRequestHandlerV2<PeriodicTaxLiabilityRequest> {

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/periodic-taxliab-request-2.xsd");
	}

	@Override
	public TaxLiabilityControlResult<TypeAssujettissement> doControl(PeriodicTaxLiabilityRequest request, @NotNull Tiers tiers) throws ControlRuleException {
		final int periode = request.getFiscalPeriod();
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		return getTaxliabilityControlService().doControlOnPeriod(tiers, periode, rechercheMenageCommun, rechercheParents, false, null);
	}
}