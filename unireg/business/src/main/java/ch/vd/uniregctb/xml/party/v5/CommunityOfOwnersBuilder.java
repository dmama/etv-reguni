package ch.vd.uniregctb.xml.party.v5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.xml.EnumHelper;

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
		final DroitProprieteCommunauteRF droitCommunaute = getDroitProprieteCommunaute(communaute);

		final CommunityOfOwners community = new CommunityOfOwners();
		community.setId(communaute.getId());
		community.setType(EnumHelper.coreToXMLv5(communaute.getType()));
		community.getMembers().addAll(buildMembers(membreInfo));
		// [SIFISC-24457] on expose le droit de propriété de la communauté
		community.setLandRight(buildLandRight(droitCommunaute, ctbIdProvider));
		return community;
	}

	@Nullable
	private static DroitProprieteCommunauteRF getDroitProprieteCommunaute(@NotNull CommunauteRF communaute) {

		final List<DroitProprieteRF> droits = communaute.getDroitsPropriete().stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toList());
		if (droits.size() > 1) {
			throw new IllegalArgumentException("La communauté idRF=[" + communaute.getIdRF() + "] possède plusieurs droits count=[" + droits.size() + "].");
		}

		final DroitProprieteRF droit = droits.stream().findFirst().orElse(null);
		if (droit != null && !(droit instanceof DroitProprieteCommunauteRF)) {
			throw new IllegalArgumentException("La communauté idRF=[" + communaute.getIdRF() + "] possède un droit qui n'est pas du bon type class=[" + droit.getClass().getSimpleName() + "].");
		}

		return (DroitProprieteCommunauteRF) droit;
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
		final List<RightHolder> membres = new ArrayList<>(membreInfo.getCount());
		// SIFISC-23747 : on retourne les membres rapprochés en premier, puis les membres non-rapprochés.
		membreInfo.getCtbIds().forEach(i -> membres.add(new RightHolder(i.intValue(), null, null, null, 0, null)));
		membreInfo.getTiersRF().forEach(t -> membres.add(new RightHolder(null, null, null, RightHolderBuilder.buildRightHolderIdentity(t), 0, null)));
		return membres;
	}
}
