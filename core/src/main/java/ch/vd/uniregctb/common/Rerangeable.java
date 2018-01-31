package ch.vd.uniregctb.common;

import ch.vd.registre.base.date.DateRange;

/**
 * @author Raphaël Marmier, 2016-01-06, <raphael.marmier@vd.ch>
 */
public interface Rerangeable<T extends DateRange> extends DateRange {

	/**
	 * Retourne un nouveau DateRange résultant de la reprise des limites du DateRange passé en paramètre.
	 * @param range Le DateRange fournissant les nouvelles dates de début et de fin.
	 * @return Une nouvelle entité avec les nouvelles dates.
	 */
	T rerange(DateRange range);
}
