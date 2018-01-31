package ch.vd.uniregctb.evenement.party;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.party.taxliab.periodic.v3.PeriodicTaxLiabilityRequest;
import ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;
import ch.vd.uniregctb.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.EnumHelper;

public class PeriodicTaxLiabilityRequestHandlerV3 extends TaxLiabilityRequestHandlerV3<PeriodicTaxLiabilityRequest> {

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/periodic-taxliab-request-3.xsd");
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
