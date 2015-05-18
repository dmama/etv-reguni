package ch.vd.uniregctb.migration.pm.historizer.container;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;

import static ch.vd.registre.base.date.RegDateHelper.dateToDisplayString;
import static ch.vd.registre.base.date.RegDateHelper.isBetween;

/**
 * Container de données historisées, i.e. avec des plages de validité
 * @param <T> type de la donnée historisée
 */
public class DateRanged<T> implements DateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final T payload;

	public DateRanged(RegDate dateDebut, RegDate dateFin, T payload) {
		ensureValidRange(dateDebut, dateFin);
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.payload = payload;
	}

	private void ensureValidRange(RegDate dateDebut, RegDate dateFin) {
		if (dateFin != null && dateDebut.isAfterOrEqual(dateFin)) {
			errorInvalid(dateDebut, dateFin);
		}
	}

	private void errorInvalid(RegDate dateDebut, RegDate dateFin) {
		throw new RuntimeException(
				String.format("Tentative de créer une période dont le début [%s] commence après ou en même temps que la fin [%s].",
				              dateToDisplayString(dateDebut),
				              dateToDisplayString(dateFin)));
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
		return isBetween(date, this.dateDebut, this.dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * @return la donnée valide entre les dates de la plage (dates de début et de fin incluses)
	 */
	public T getPayload() {
		return payload;
	}
}
