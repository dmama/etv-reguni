package ch.vd.unireg.registrefoncier;

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Tiers;

/**
 * Information d'appartenance d'un ayant-droit (= tiersRF ou héritier fiscal) dans une communauté RF.
 */
public class CommunauteRFAppartenanceInfo implements DateRange {

	/**
	 * Comparateur qui trie par :
	 * <ol>
	 *     <li>ordre chronologique croissant</li>
	 *     <li>ordre de numéro de CTB croissant (les nuls à la fin)</li>
	 *     <li>ordre d'id de tiers RF croissant (les nuls à la fin)</li>
	 * </ol>
	 */
	public static final Comparator<CommunauteRFAppartenanceInfo> COMPARATOR = new DateRangeComparator<CommunauteRFAppartenanceInfo>()
			                               .thenComparing(CommunauteRFAppartenanceInfo::getCtbId, Comparator.nullsLast(Comparator.naturalOrder()))
			                               .thenComparing(CommunauteRFAppartenanceInfo::getAyantDroit, Comparator.nullsLast(Comparator.comparing(AyantDroitRF::getId)));
	@Nullable
	private final RegDate dateDebut;
	@Nullable
	private final RegDate dateFin;
	@Nullable
	private final Date annulationDate;

	/**
	 * L'ayant-droit (nul s'il s'agit d'un héritier défini fiscalement)
	 */
	@Nullable
	private final TiersRF ayantDroit;

	/**
	 * Numéro de contribuable de l'ayant-droit (nul s'il s'agit d'un tiers RF non-rapproché)
	 */
	@Nullable
	private final Long ctbId;

	public CommunauteRFAppartenanceInfo(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, @Nullable Date annulationDate, @Nullable TiersRF ayantDroit, @Nullable Long ctbId) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.annulationDate = annulationDate;
		this.ayantDroit = ayantDroit;
		this.ctbId = ctbId;
	}

	public CommunauteRFAppartenanceInfo(@NotNull DroitProprietePersonneRF droit) {
		this.dateDebut = droit.getDateDebutMetier();
		this.dateFin = droit.getDateFinMetier();
		this.annulationDate = droit.getAnnulationDate();
		this.ayantDroit = (TiersRF) droit.getAyantDroit();
		this.ctbId = Optional.of(droit.getAyantDroit())
				.filter(TiersRF.class::isInstance)
				.map(TiersRF.class::cast)
				.map(TiersRF::getCtbRapproche)
				.map(Tiers::getId)
				.orElse(null);
	}

	@Override
	@Nullable
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	@Nullable
	public RegDate getDateFin() {
		return dateFin;
	}

	@Nullable
	public Date getAnnulationDate() {
		return annulationDate;
	}

	@Nullable
	public TiersRF getAyantDroit() {
		return ayantDroit;
	}

	@Nullable
	public Long getCtbId() {
		return ctbId;
	}
}
