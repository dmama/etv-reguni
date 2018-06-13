package ch.vd.unireg.xml.party.v5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.unireg.registrefoncier.CommunauteRFPrincipalInfo;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityLeader;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;

public abstract class CommunityOfOwnersBuilder {

	private CommunityOfOwnersBuilder() {
	}

	public static CommunityOfOwners newCommunity(@NotNull CommunauteRF communaute,
	                                             @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider,
	                                             @NotNull Function<CommunauteRF, CommunauteRFMembreInfo> membreInfoProvider) {

		final CommunauteRFMembreInfo membreInfo = membreInfoProvider.apply(communaute);
		if (membreInfo == null) {
			throw new ProgrammingException("Les informations de la communauté id=[" + communaute.getId() + "] sont nulles.");
		}
		final List<LandOwnershipRight> droitsCommunaute = buildLandRights(communaute, ctbIdProvider);
		final LandOwnershipRight dernierDroitCommunaute = (droitsCommunaute.isEmpty() ? null : CollectionsUtils.getLastElement(droitsCommunaute));

		final CommunityOfOwners community = new CommunityOfOwners();
		community.setId(communaute.getId());
		community.setType(EnumHelper.coreToXMLv5(communaute.getType()));
		community.getMembers().addAll(buildMembers(membreInfo));
		// [SIFISC-24457] on expose le droit de propriété de la communauté
		// [IMM-1215] une communauté peut avoir plusieurs droits, pour garder la compatibilité ascendante, on expose le dernier droit
		community.setLandRight(dernierDroitCommunaute);
		community.getLeaders().addAll(buildLeaders(membreInfo.getPrincipaux()));
		// [SIFISC-28067] on expose l'historique de l'appartenance des membres de la communauté
		community.getMemberships().addAll(MembershipBuilder.buildCommunityOfOwnerMemberships(membreInfo.getMembresHisto()));
		// [IMM-1215] on expose les droits de propriété de la communauté
		community.getLandRights().addAll(droitsCommunaute);
		return community;
	}

	@NotNull
	private static List<LandOwnershipRight> buildLandRights(@NotNull CommunauteRF communaute, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		return communaute.getDroitsPropriete().stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(DroitProprieteCommunauteRF.class::cast)
				.sorted(new DroitRFRangeMetierComparator())
				.map(droit -> buildLandRight(droit, ctbIdProvider))
				.collect(Collectors.toList());
	}

	@Nullable
	private static LandOwnershipRight buildLandRight(@Nullable DroitProprieteCommunauteRF droitCommunaute, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		if (droitCommunaute == null) {
			return null;
		}
		else {
			return LandRightBuilder.newLandOwnershipRight(droitCommunaute, ctbIdProvider);
		}
	}

	@NotNull
	private static Collection<? extends RightHolder> buildMembers(@NotNull CommunauteRFMembreInfo membreInfo) {
		final List<RightHolder> membres = new ArrayList<>();
		// SIFISC-23747 : on retourne les membres rapprochés en premier, puis les membres non-rapprochés.
		membreInfo.getCtbIds().forEach(i -> membres.add(new RightHolder(i.intValue(), null, null, null, 0, null)));
		membreInfo.getTiersRF().forEach(t -> membres.add(new RightHolder(null, null, null, RightHolderBuilder.buildRightHolderIdentity(t), 0, null)));
		return membres;
	}

	@NotNull
	private static List<CommunityLeader> buildLeaders(@NotNull List<CommunauteRFPrincipalInfo> principaux) {
		return principaux.stream()
				.map(p -> new CommunityLeader(DataHelper.coreToXMLv2(p.getDateDebut()),
				                              DataHelper.coreToXMLv2(p.getDateFin()),
				                              (int) p.getCtbId(),
				                              0,
				                              null))
				.collect(Collectors.toList());
	}

}
