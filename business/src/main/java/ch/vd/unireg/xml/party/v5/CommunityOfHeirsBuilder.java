package ch.vd.uniregctb.xml.party.v5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirLeader;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirMember;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.DataHelper;

public abstract class CommunityOfHeirsBuilder {

	private CommunityOfHeirsBuilder() {
	}

	@Nullable
	public static CommunityOfHeirs newCommunity(@NotNull Tiers tiers) {

		final List<Heritage> liensHeritage = tiers.getRapportsObjet().stream()
				.filter(Heritage.class::isInstance)
				.map(Heritage.class::cast)
				.collect(Collectors.toList());

		if (liensHeritage.stream()
				.filter(AnnulableHelper::nonAnnule)
				.count() < 2) {
			// zéro ou un seul héritier, il n'y a pas de communauté
			return null;
		}

		final RegDate dateDebutHeritage = liensHeritage.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(Heritage::getDateDebut)
				.min(RegDate::compareTo)
				.orElse(null);

		// à cause du flag 'principal', il arrive que deux liens d'héritage concernant le même héritier
		// représente en fait une appartenance continue de l'héritier à la communauté. Pour éviter
		// d'exposer cette appartenance continue en deux morceaux, on les fusionne ici.
		final List<Heritage> allCollated = collateHeritages(liensHeritage);
		final List<CommunityOfHeirMember> members = allCollated.stream()
				.map(CommunityOfHeirsBuilder::buildMember)
				.collect(Collectors.toList());

		final List<CommunityOfHeirLeader> leaders = liensHeritage.stream()
				.filter(h -> h.getPrincipalCommunaute() != null && h.getPrincipalCommunaute())  // on s'intéresse qu'aux principaux
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(false))
				.map(CommunityOfHeirsBuilder::buildPrincipal)
				.collect(Collectors.toList());

		final CommunityOfHeirs community = new CommunityOfHeirs();
		community.setInheritedFromNumber(tiers.getNumero().intValue());
		community.setInheritanceDateFrom(DataHelper.coreToXMLv2(dateDebutHeritage));
		community.getMembers().addAll(members);
		community.getLeaders().addAll(leaders);
		return community;
	}

	/**
	 * Fusionne ensemble les liens d'héritage qui peuvent l'être sans tenir compte du flag <i>principal de communauté.</i>. Les liens d'héritage annulés ne sont pas fusionnés.
	 *
	 * @param liensHeritage des liens d'héritage
	 * @return les liens d'héritage fusionnés
	 */
	@NotNull
	private static List<Heritage> collateHeritages(@NotNull List<Heritage> liensHeritage) {
		final List<Heritage> allCollated = new ArrayList<>(liensHeritage.size());

		// on regroupe les liens d'héritage par id d'héritier
		final Map<Long, List<Heritage>> map = liensHeritage.stream()
				.collect(Collectors.toMap(RapportEntreTiers::getSujetId, Collections::singletonList, ListUtils::union));

		for (Map.Entry<Long, List<Heritage>> entry : map.entrySet()) {
			// on trie les liens d'héritage
			final List<Heritage> uncollated = entry.getValue();
			uncollated.sort(new AnnulableHelper.AnnulableDateRangeComparator<>(false));
			// on fusionne les liens d'héritage qui peuvent l'être
			final List<Heritage> collated = DateRangeHelper.collate(uncollated, CommunityOfHeirsBuilder::isCollatable, CommunityOfHeirsBuilder::collate);
			allCollated.addAll(collated);
		}

		allCollated.sort(new AnnulableHelper.AnnulableDateRangeComparator<>(false));
		return allCollated;
	}

	@NotNull
	private static Heritage collate(Heritage r1, Heritage r2) {
		return new Heritage(r1.getDateDebut(), r2.getDateFin(), r1.getSujetId(), r1.getObjetId(), null);
	}

	private static boolean isCollatable(@NotNull Heritage r1, @NotNull Heritage r2) {
		if (!Objects.equals(r1.getObjetId(), r2.getObjetId()) || !Objects.equals(r1.getSujetId(), r2.getSujetId())) {
			throw new IllegalArgumentException("Les héritages ne concernent pas les mêmes défunts/héritiers");
		}
		return  !r1.isAnnule() && !r2.isAnnule() && // on ne fusionne pas les héritages annulés
				r1.getDateFin() != null && r1.getDateFin().getOneDayAfter() == r2.getDateDebut();
	}

	@NotNull
	private static CommunityOfHeirMember buildMember(@NotNull Heritage heritage) {
		final CommunityOfHeirMember member = new CommunityOfHeirMember();
		member.setCancellationDate(DataHelper.coreToXMLv2(heritage.getAnnulationDate()));
		member.setDateFrom(DataHelper.coreToXMLv2(heritage.getDateDebut()));
		member.setDateTo(DataHelper.coreToXMLv2(heritage.getDateFin()));
		member.setTaxPayerNumber(heritage.getSujetId().intValue());
		return member;
	}

	@NotNull
	private static CommunityOfHeirLeader buildPrincipal(@NotNull Heritage heritage) {
		final CommunityOfHeirLeader leader = new CommunityOfHeirLeader();
		leader.setCancellationDate(DataHelper.coreToXMLv2(heritage.getAnnulationDate()));
		leader.setDateFrom(DataHelper.coreToXMLv2(heritage.getDateDebut()));
		leader.setDateTo(DataHelper.coreToXMLv2(heritage.getDateFin()));
		leader.setTaxPayerNumber(heritage.getSujetId().intValue());
		return leader;
	}
}
