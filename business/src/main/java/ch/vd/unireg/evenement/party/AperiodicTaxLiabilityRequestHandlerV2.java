package ch.vd.unireg.evenement.party;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.party.control.ControlRuleException;
import ch.vd.unireg.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v2.AperiodicTaxLiabilityRequest;

public class AperiodicTaxLiabilityRequestHandlerV2 extends TaxLiabilityRequestHandlerV2<AperiodicTaxLiabilityRequest> {

	@Override
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("unireg-common-1.xsd",
		                     "event/party/request-1.xsd",
		                     "event/party/taxliab-request-2.xsd",
		                     "event/party/aperiodic-taxliab-request-2.xsd");
	}

	@Override
	public TaxLiabilityControlResult<ModeImposition> doControl(AperiodicTaxLiabilityRequest request, @NotNull Tiers tiers) throws ControlRuleException {
		final RegDate dateControle = DataHelper.xmlToCore(request.getDate());
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		return getTaxliabilityControlService().doControlOnDate(tiers, dateControle, rechercheMenageCommun, rechercheParents, false,null);
	}
}
