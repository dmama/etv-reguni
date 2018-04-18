package ch.vd.unireg.xml.party.v5;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.registrefoncier.BeneficeServitudeRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.CommunauteRFAppartenanceInfo;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnerMembership;
import ch.vd.unireg.xml.party.landregistry.v1.EasementEncumbrance;
import ch.vd.unireg.xml.party.landregistry.v1.EasementMembership;

public class MembershipBuilder {
	private MembershipBuilder() {
	}

	@NotNull
	public static List<CommunityOfOwnerMembership> buildCommunityOfOwnerMemberships(@NotNull List<CommunauteRFAppartenanceInfo> membresHisto) {
		return membresHisto.stream()
				.map(m -> new CommunityOfOwnerMembership(DataHelper.coreToXMLv2(m.getDateDebut()),
				                                         DataHelper.coreToXMLv2(m.getDateFin()),
				                                         DataHelper.coreToXMLv2(m.getAnnulationDate()),
				                                         RightHolderBuilder.getRightHolder(m),
				                                         null))
				.collect(Collectors.toList());
	}

	@NotNull
	public static EasementMembership buildEasementMembership(@NotNull BeneficeServitudeRF benefice, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		return new EasementMembership(DataHelper.coreToXMLv2(benefice.getDateDebut()),
		                              DataHelper.coreToXMLv2(benefice.getDateFin()),
		                              DataHelper.coreToXMLv2(benefice.getAnnulationDate()),
		                              RightHolderBuilder.getRightHolder(benefice.getAyantDroit(), ctbIdProvider),
		                              null);
	}

	@NotNull
	public static EasementEncumbrance buildEasementEncumbrance(@NotNull ChargeServitudeRF charge) {
		return new EasementEncumbrance(DataHelper.coreToXMLv2(charge.getDateDebut()),
		                              DataHelper.coreToXMLv2(charge.getDateFin()),
		                              DataHelper.coreToXMLv2(charge.getAnnulationDate()),
		                              charge.getImmeuble().getId(),
		                              null);
	}
}
