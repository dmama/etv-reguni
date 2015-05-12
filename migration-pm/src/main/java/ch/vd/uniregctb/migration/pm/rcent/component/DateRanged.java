package ch.vd.uniregctb.migration.pm.rcent.component;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Container de données historisées, i.e. avec des plages de validité
 * @param <T> type de la donnée historisée
 */
public class DateRanged<T> implements DateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final T payload;

	public DateRanged(RegDate dateDebut, RegDate dateFin, T payload) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.payload = payload;
	}

	@NotNull
	public DateRanged<T> withDateFin(RegDate dateFin) {
		return new DateRanged<>(this.dateDebut, dateFin, this.payload);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, this.dateDebut, this.dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * @return la donnée valide entre les dates de la plage (dates de début et de fin incluses)
	 */
	public T getPayload() {
		return payload;
	}
}
