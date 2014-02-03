package ch.vd.uniregctb.evenement.party;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v3.AperiodicTaxLiabilityRequest;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;
import ch.vd.uniregctb.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.DataHelper;

public class AperiodicTaxLiabilityRequestHandlerV3 extends TaxLiabilityRequestHandlerV3<AperiodicTaxLiabilityRequest> {

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/aperiodic-taxliab-request-3.xsd");
	}

	@Override
	public TaxLiabilityControlResult doControl(AperiodicTaxLiabilityRequest request, @NotNull Tiers tiers) throws ControlRuleException {
		final RegDate dateControle = DataHelper.xmlToCore(request.getDate());
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		return getTaxliabilityControlService().doControlOnDate(tiers, dateControle, rechercheMenageCommun, rechercheParents, true);
	}
}
