package ch.vd.unireg.interfaces.organisation.data;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

/**
 * Interface implémentée par les entités qui sont capable de se limiter sur une plage de dates
 * @param <T> type de la valeur après limitation
 */
public interface DateRangeLimitable<T extends DateRange> {

	/**
	 * @param dateDebut date de début de la limitation (si <code>null</code>, pas de limitation ici)
	 * @param dateFin date de fin de la limitation (si <code>null</code>, pas de limitation ici)
	 * @return une nouvelle instance d'entité limitée (au plus) aux dates demandées
	 */
	T limitTo(RegDate dateDebut, RegDate dateFin);
}
