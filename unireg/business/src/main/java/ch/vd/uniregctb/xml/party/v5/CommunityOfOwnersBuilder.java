package ch.vd.uniregctb.xml.party.v5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.xml.EnumHelper;

public abstract class CommunityOfOwnersBuilder {

	private CommunityOfOwnersBuilder() {
	}

	public static CommunityOfOwners newCommunity(CommunauteRF communaute, Function<Long, CommunauteRFMembreInfo> membreInfoProvider) {

		final CommunauteRFMembreInfo membreInfo = membreInfoProvider.apply(communaute.getId());
		if (membreInfo == null) {
			throw new ProgrammingException("Les informations de la communauté id=[" + communaute.getId() + "] sont nulles.");
		}

		final CommunityOfOwners community = new CommunityOfOwners();
		community.setId(communaute.getId());
		community.setType(EnumHelper.coreToXMLv5(communaute.getType()));
		community.getMembers().addAll(buildMembers(membreInfo));
		return community;
	}

	@NotNull
	private static Collection<? extends RightHolder> buildMembers(@NotNull CommunauteRFMembreInfo membreInfo) {
		final List<RightHolder> membres = new ArrayList<>(membreInfo.getCount());
		// SIFISC-23747 : on retourne les membres rapprochés en premier, puis les membres non-rapprochés.
		membreInfo.getCtbIds().forEach(i -> membres.add(new RightHolder(i.intValue(), null, 0, null)));
		membreInfo.getTiersRF().forEach(t -> membres.add(new RightHolder(null, RightHolderBuilder.buildRightHolderIdentity(t), 0, null)));
		return membres;
	}
}
