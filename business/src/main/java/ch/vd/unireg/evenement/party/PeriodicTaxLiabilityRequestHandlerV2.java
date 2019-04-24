package ch.vd.unireg.evenement.party;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.party.control.ControlRuleException;
import ch.vd.unireg.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.event.party.taxliab.periodic.v2.PeriodicTaxLiabilityRequest;

public class PeriodicTaxLiabilityRequestHandlerV2 extends TaxLiabilityRequestHandlerV2<PeriodicTaxLiabilityRequest> {

	@Override
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("unireg-common-1.xsd",
		                     "event/party/request-1.xsd",
		                     "event/party/taxliab-request-2.xsd",
		                     "event/party/periodic-taxliab-request-2.xsd");
	}

	@Override
	public TaxLiabilityControlResult<TypeAssujettissement> doControl(PeriodicTaxLiabilityRequest request, @NotNull Tiers tiers) throws ControlRuleException {
		final int periode = request.getFiscalPeriod();
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		return getTaxliabilityControlService().doControlOnPeriod(tiers, periode, rechercheMenageCommun, rechercheParents, false, null);
	}
}
