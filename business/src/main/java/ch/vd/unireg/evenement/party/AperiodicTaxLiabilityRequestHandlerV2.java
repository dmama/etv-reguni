package ch.vd.unireg.evenement.party;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v2.AperiodicTaxLiabilityRequest;
import ch.vd.unireg.evenement.party.control.ControlRuleException;
import ch.vd.unireg.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.xml.DataHelper;

public class AperiodicTaxLiabilityRequestHandlerV2 extends TaxLiabilityRequestHandlerV2<AperiodicTaxLiabilityRequest> {

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/aperiodic-taxliab-request-2.xsd");
	}

	@Override
	public TaxLiabilityControlResult<ModeImposition> doControl(AperiodicTaxLiabilityRequest request, @NotNull Tiers tiers) throws ControlRuleException {
		final RegDate dateControle = DataHelper.xmlToCore(request.getDate());
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		return getTaxliabilityControlService().doControlOnDate(tiers, dateControle, rechercheMenageCommun, rechercheParents, false,null);
	}
}
