package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Container de données historisées, i.e. avec des plages de validité. Brique de base permettant
 * d'ajouter la dimension temporelle à une donnée.
 *
 * Une plage sans date de début indique une plage dont le commencement se situe
 * à une date incertaine dans le passé. Elle peut être en cours ou terminée selon la date de
 * fin.
 *
 * Une plage sans date de fin représente une plage en cours de validité ou non encore commencée, selon
 * l'état de la date de début.
 *
 * Une plage sans date représente une plage aux contours incertains, en cours de validité.
 *
 * Une plage peut ne durer qu'un seul jour. Dans ce cas les dates de début et de fin coincident.
 *
 * Il faut noter les contraintes suivantes, imposée par cette implémentation:
 * - La date de début ne peut en aucun cas se situer après la date de fin.
 * - Une plage doit obligatoirement référencer une payload. On est ici dans une approche Decorator.
 *   On représente une entité à laquelle on ajoute une caractéristique temporelle. Une instance n'a
 *   pas donc de sens sans charge utile. L'absence de valeur est représentée par une absence de donnée,
 *   et jamais par une plage vide (c'est un choix de conception).
 *
 * @param <T> type de la donnée historisée
 */
public class DateRanged<T> implements DateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	@NotNull
	private final T payload;

	/**
	 * Crée une nouvelle instance immuable et valide.
	 *
	 * @param dateDebut La date de début. Facultative.
	 * @param dateFin La date de fin. Facultative.
	 * @param payload La données. Obligatoire.
	 */
	public DateRanged(RegDate dateDebut, RegDate dateFin, @NotNull T payload) {
		if (payload == null) {
			throw new NullPointerException("Tentative de créer une plage sans charge utile! Une plage temporelle doit obligatoirement porter sur une donnée.");
		}
		ensureValidRange(dateDebut, dateFin);
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.payload = payload;
	}

	private void ensureValidRange(RegDate dateDebut, RegDate dateFin) {
		if (dateFin != null && dateDebut.isAfter(dateFin)) {
			throw new IllegalArgumentException (
					String.format("Tentative de créer une plage dont le début [%s] commence après la fin [%s].",
								  RegDateHelper.dateToDisplayString(dateDebut),
								  RegDateHelper.dateToDisplayString(dateFin)));
		}
	}

	/**
	 * Crée une nouvelle plage dotée de la date de fin précisée en paramètre.
	 * @param dateFin La nouvelle date de fin.
	 * @return La nouvelle plage.
	 */
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
	@NotNull
	public T getPayload() {
		return payload;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final DateRanged<?> that = (DateRanged<?>) o;

		if (!getDateDebut().equals(that.getDateDebut())) return false;
		if (getDateFin() != null ? !getDateFin().equals(that.getDateFin()) : that.getDateFin() != null) return false;
		return getPayload().equals(that.getPayload());

	}

	@Override
	public int hashCode() {
		int result = getDateDebut().hashCode();
		result = 31 * result + (getDateFin() != null ? getDateFin().hashCode() : 0);
		result = 31 * result + getPayload().hashCode();
		return result;
	}
}
