package ch.vd.unireg.registrefoncier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.tiers.CommunauteHeritiers;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.RapportEntreTiers;

/**
 * Les informations minimales sur les membres d'une communauté d'un point-de-vue Unireg.
 */
public class CommunauteRFMembreInfo {

	/**
	 * Les ids des tiers Unireg qui correspondent aux tiers RF de la communauté (donc, en passant par la table de rapprochement).
	 * <p>
	 * Le nombre d'éléments dans cette collection ne contient pas tiers RF non-rapprochés avec des tiers Unireg.
	 */
	@NotNull
	private final List<Long> ctbIds;

	/**
	 * L'historique des principaux de la communauté
	 */
	private final List<CommunauteRFPrincipalInfo> principaux;

	/**
	 * Les tiers RF des membres de la communauté qui n'ont pas été rapprochés avec des tiers Unireg.
	 */
	@NotNull
	private final List<TiersRF> tiersRF;

	/**
	 * L'historique de l'appartenance des membres.
	 */
	private final List<CommunauteRFAppartenanceInfo> membresHisto;

	public CommunauteRFMembreInfo(@NotNull Collection<Long> ctbIds, @NotNull Collection<TiersRF> tiersRF, @NotNull Collection<CommunauteRFAppartenanceInfo> membresHisto) {
		this.ctbIds = new ArrayList<>(ctbIds);
		this.tiersRF = new ArrayList<>(tiersRF);
		this.membresHisto = new ArrayList<>(membresHisto);
		this.principaux = new ArrayList<>();

		// on trie l'historique des membres par :
		//   1. ordre chronologique croissant
		//   2. ordre de numéro de CTB croissant (les nuls à la fin)
		//   3. ordre d'id de tiers RF croissant (les nuls à la fin)
		this.membresHisto.sort(CommunauteRFAppartenanceInfo.COMPARATOR);
	}

	/**
	 * Remplace les membres défunts par leurs héritiers en fonction des communautés d'héritiers spécifiées.
	 *
	 * @param communautesHeritiers les communautés d'héritiers qui concernent les membres de la communauté
	 * @return une nouvelle instance des infos de la communauté
	 */
	@NotNull
	public CommunauteRFMembreInfo applyHeritages(@NotNull Map<Long, CommunauteHeritiers> communautesHeritiers) {

		final Set<Long> membresIds = new HashSet<>(this.ctbIds);

		// on met-à-jour les ids des membres
		for (Map.Entry<Long, CommunauteHeritiers> entry : communautesHeritiers.entrySet()) {
			// on supprime le défunt
			if (membresIds.remove(entry.getKey())) {
				// on ajoute les héritiers
				final Set<Long> heritiers = entry.getValue().getLiensHeritage().stream()
						.map(Heritage::getSujetId)
						.collect(Collectors.toSet());
				membresIds.addAll(heritiers);
			}
		}

		// on met-à-jour l'historique de l'appartenance des membres
		final List<CommunauteRFAppartenanceInfo> membresHistoAvecHeritiers = new ArrayList<>();
		for (CommunauteRFAppartenanceInfo appartenance : this.membresHisto) {

			final Long ctbId = appartenance.getCtbId();
			if (ctbId == null) {
				// le membre n'est pas rapproché, rien à faire de spécial
				membresHistoAvecHeritiers.add(appartenance);
				continue;
			}

			final CommunauteHeritiers heritiers = communautesHeritiers.get(ctbId);
			if (heritiers == null) {
				// le membre ne possède pas d'héritiers fiscaux, rien à faire de spécial
				membresHistoAvecHeritiers.add(appartenance);
				continue;
			}

			final DateRange intersection = DateRangeHelper.intersection(appartenance, heritiers);
			if (intersection == null) {
				// l'appartenance ne correspond pas à une période d'héritage fiscal, rien à faire de spécial
				membresHistoAvecHeritiers.add(appartenance);
				continue;
			}

			// l'appartenance du membre à la communauté est active pendant la période d'existance des héritiers :

			// 1) on réduit ou remplace l'appartenance du défunt par celles de ses héritiers
			final List<CommunauteRFAppartenanceInfo> appartenanceReduite = DateRangeHelper.subtract(appartenance, Collections.singletonList(heritiers), (range, debut, fin) -> {
				final RegDate dateDebut = debut != null ? debut : range.getDateDebut();
				final RegDate dateFin = fin != null ? fin : range.getDateFin();
				return new CommunauteRFAppartenanceInfo(dateDebut, dateFin, null, range.getAyantDroit(), range.getCtbId());
			});
			membresHistoAvecHeritiers.addAll(appartenanceReduite);

			// 2) on ajoute tous les héritiers en remplacement de l'appartenance du défunt
			final List<CommunauteRFAppartenanceInfo> appartenancesDesHeritiers = collateLiensHeritage(heritiers.getLiensHeritage()).stream()
					.filter(AnnulableHelper::nonAnnule)
					.map(heritage -> new CommunauteRFAppartenanceInfo(intersection.getDateDebut(), intersection.getDateFin(), null, null, heritage.getSujetId()))
					.collect(Collectors.toList());
			membresHistoAvecHeritiers.addAll(appartenancesDesHeritiers);
		}

		return new CommunauteRFMembreInfo(membresIds, this.tiersRF, membresHistoAvecHeritiers);
	}

	/**
	 * Cette méthode regroupe les liens d'héritage par héritier et les fusionne s'ils se touchent et sans tenir compte du flag 'principal'.
	 *
	 * @param liensHeritage des liens d'héritage dans n'importe quel ordre
	 * @return les liens d'héritage fusionnés lorsque c'était possible.
	 */
	@NotNull
	private static List<Heritage> collateLiensHeritage(@NotNull List<Heritage> liensHeritage) {
		if (liensHeritage.size() <= 1) {
			// inutile de se compliquer la vie
			return liensHeritage;
		}

		// on regroupe les héritages par héritiers
		final Map<Long, List<Heritage>> map = liensHeritage.stream()
				.sorted(new DateRangeComparator<>())
				.collect(Collectors.toMap(RapportEntreTiers::getSujetId, Collections::singletonList, ListUtils::union));

		// on fusionne tous les périodes d'héritage avec les mêmes héritiers qui se touchent
		return map.values().stream()
				.map(list -> DateRangeHelper.collate(list, DateRangeHelper::isCollatable, CommunauteRFMembreInfo::collateHeritages))
				.flatMap(Collection::stream)
				.sorted(new DateRangeComparator<>())
				.collect(Collectors.toList());
	}

	@NotNull
	private static Heritage collateHeritages(@NotNull Heritage l, @NotNull Heritage r) {
		if (!l.getSujetId().equals(r.getSujetId())) {
			throw new IllegalArgumentException("Les héritiers sont différents.");
		}
		if (!l.getObjetId().equals(r.getObjetId())) {
			throw new IllegalArgumentException("Les défunts sont différents.");
		}
		return new Heritage(l.getDateDebut(), r.getDateFin(), l.getSujetId(), l.getObjetId(), null);
	}

	/**
	 * Trie les membres de la communautés (identifiés par leurs ids de contribuables, les tiers RF non-rapprochés ne sont pas triés).
	 *
	 * @param comparator le comparateur à utiliser pour le tri.
	 */
	public void sortMembers(Comparator<Long> comparator) {
		this.ctbIds.sort(comparator);
	}

	@NotNull
	public List<Long> getCtbIds() {
		return ctbIds;
	}

	@NotNull
	public List<TiersRF> getTiersRF() {
		return tiersRF;
	}

	public void setPrincipaux(List<CommunauteRFPrincipalInfo> principaux) {
		this.principaux.addAll(principaux);
	}

	@NotNull
	public List<CommunauteRFPrincipalInfo> getPrincipaux() {
		return principaux;
	}

	/**
	 * @return l'historique de
	 */
	@NotNull
	public List<CommunauteRFAppartenanceInfo> getMembresHisto() {
		return membresHisto;
	}
}
