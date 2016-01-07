package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.DateRange;

/**
 * DateRange servant à représenter une valeur entrant dans un historique multi-source résultant d'une surcharge.
 * La source est ainsi connue pour chaque période d'historique.
 *
 * @author Raphaël Marmier, 2016-01-06, <raphael.marmier@vd.ch>
 */
public interface Sourced<T extends Enum> extends DateRange {

	/**
	 * @return La source de la valeur portée par le DateRange.
	 */
	T getSource();
}
