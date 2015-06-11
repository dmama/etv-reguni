package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.function.Function;

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
		ensureValidRange(dateDebut, dateFin);
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.payload = payload;
	}

	private void ensureValidRange(RegDate dateDebut, RegDate dateFin) {
		if (dateFin != null && dateDebut.isAfter(dateFin)) {
			throw new IllegalArgumentException (
					String.format("Tentative de créer une période dont le début [%s] commence après ou en même temps que la fin [%s].",
					              RegDateHelper.dateToDisplayString(dateDebut),
					              RegDateHelper.dateToDisplayString(dateFin)));
		}
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

	/**
	 * @param mapper transformation à appliquer à la payload
	 * @param <U> type de la payload de l'objet retourné
	 * @return un nouvel objet DateRanged, valide aux mêmes dates, et dont la payload est constuite à partir de la payload courante au travers de la fonction de mapping
	 */
	public <U> DateRanged<U> map(Function<? super T, ? extends U> mapper) {
		return new DateRanged<>(dateDebut, dateFin, mapper.apply(payload));
	}
}
