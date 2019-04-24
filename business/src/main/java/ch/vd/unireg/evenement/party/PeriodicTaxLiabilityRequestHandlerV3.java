package ch.vd.unireg.evenement.party;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.party.control.ControlRuleException;
import ch.vd.unireg.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.event.party.taxliab.periodic.v3.PeriodicTaxLiabilityRequest;
import ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType;

public class PeriodicTaxLiabilityRequestHandlerV3 extends TaxLiabilityRequestHandlerV3<PeriodicTaxLiabilityRequest> {

	@Override
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("unireg-common-1.xsd",
		                     "unireg-common-2.xsd",
		                     "party/unireg-party-taxresidence-2.xsd",
		                     "event/party/request-1.xsd",
		                     "event/party/taxliab-request-2.xsd",
		                     "event/party/periodic-taxliab-request-3.xsd");
	}

	@Override
	public TaxLiabilityControlResult<TypeAssujettissement> doControl(PeriodicTaxLiabilityRequest request, @NotNull Tiers tiers) throws ControlRuleException {
		final int periode = request.getFiscalPeriod();
		final boolean rechercheMenageCommun = request.isSearchCommonHouseHolds();
		final boolean rechercheParents = request.isSearchParents();
		Set<TypeAssujettissement> assujettissementARejeter = getTypeAssujetissementARejeter(request.getIndividualTaxLiabilityToReject());
		return getTaxliabilityControlService().doControlOnPeriod(tiers, periode, rechercheMenageCommun, rechercheParents, true, assujettissementARejeter);
	}
	private Set<TypeAssujettissement> getTypeAssujetissementARejeter(List<IndividualTaxLiabilityType> taxliabilityToReject) {
		if (taxliabilityToReject.isEmpty()) {
			return null;
		}
		final Set<TypeAssujettissement> result = EnumSet.noneOf(TypeAssujettissement.class);
		for (IndividualTaxLiabilityType taxliability : taxliabilityToReject) {
			result.add(EnumHelper.xmlToCore(taxliability));
		}
		return result;
	}
}
