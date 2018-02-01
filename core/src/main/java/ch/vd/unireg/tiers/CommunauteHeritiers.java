package ch.vd.uniregctb.tiers;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

/**
 * Informations sur une communauté d'héritiers Unireg.
 */
public class CommunauteHeritiers implements DateRange {

	private final long defuntId;
	@Nullable
	private final RegDate dateDebut;
	@Nullable
	private final RegDate dateFin;
	@NotNull
	private final List<Heritage> liensHeritage;

	/**
	 * Crée une communauté d'héritiers composée d'un seul lien d'héritage.
	 */
	public CommunauteHeritiers(@NotNull Heritage heritage) {
		this.defuntId = heritage.getObjetId();
		this.liensHeritage = Collections.singletonList(heritage);
		this.dateDebut = heritage.getDateDebut();
		this.dateFin = heritage.getDateFin();
	}

	/**
	 * Crée une communauté d'héritiers composée de plusieurs liens d'héritage.
	 */
	public CommunauteHeritiers(long defuntId, @NotNull List<Heritage> liensHeritage) {
		this.defuntId = defuntId;
		this.liensHeritage = liensHeritage;
		final DateRange range = DateRangeHelper.getOverallRange(liensHeritage);
		this.dateDebut = (range == null ? null : range.getDateDebut());
		this.dateFin = (range == null ? null : range.getDateFin());
	}

	/**
	 * Fusionne deux communautés d'héritiers en une. Le défunt doit être le même.
	 */
	public static CommunauteHeritiers merge(@NotNull CommunauteHeritiers left, @NotNull CommunauteHeritiers right) {
		if (left.getDefuntId() != right.getDefuntId()) {
			throw new IllegalArgumentException();
		}
		return new CommunauteHeritiers(left.getDefuntId(), ListUtils.union(left.getLiensHeritage(), right.getLiensHeritage()));
	}

	/**
	 * @return le numéro de contribuable du défunt.
	 */
	public long getDefuntId() {
		return defuntId;
	}

	/**
	 * @return la date de début de l'héritage (= date la plus ancienne des rapports d'héritage)
	 */
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

	@NotNull
	public List<Heritage> getLiensHeritage() {
		return liensHeritage;
	}
}
