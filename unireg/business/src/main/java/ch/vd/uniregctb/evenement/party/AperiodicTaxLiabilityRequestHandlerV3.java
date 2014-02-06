package ch.vd.uniregctb.evenement.party;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v3.AperiodicTaxLiabilityRequest;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;
import ch.vd.uniregctb.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

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
		final List<TaxationMethod> taxationMethodToReject= request.getTaxationMethodToReject();
		Set<ModeImposition> modeImpositionARejeter = getModeImpositionARejeter(taxationMethodToReject);
		return getTaxliabilityControlService().doControlOnDate(tiers, dateControle, rechercheMenageCommun, rechercheParents, true, modeImpositionARejeter);
	}

	private Set<ModeImposition> getModeImpositionARejeter(List<TaxationMethod> taxationMethodToReject) {
		if (taxationMethodToReject.isEmpty()) {
			return null;
		}
		final List<ModeImposition> result = new ArrayList<>();
		for (TaxationMethod taxationMethod : taxationMethodToReject) {
			 result.add(EnumHelper.xmlToCore(taxationMethod));
		}
		return EnumSet.copyOf(result);
	}
}
