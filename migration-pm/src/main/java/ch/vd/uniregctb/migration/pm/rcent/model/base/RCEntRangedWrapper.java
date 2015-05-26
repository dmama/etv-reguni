package ch.vd.uniregctb.migration.pm.rcent.model.base;

import ch.vd.registre.base.date.RegDate;

/**
 * Englobe un objet de donnée en lui associant une période de validité.
 *
 * L'objectif de classe est de faciliter la prise en charge de l'API RCEnt. Il est plus facile
 * de maintenir les méthodes déléguées que de s'assurer de la copie de chaque champ séparément.
 *
 * @param <E>
 */
public abstract class RCEntRangedWrapper<E> extends RCEntRangedElement {
	private final E element;

	public RCEntRangedWrapper(RegDate beginDate, RegDate endDateDate, E element) {
		super(beginDate, endDateDate);
		this.element = element;
	}

	protected E getElement() {
		return element;
	}
}
