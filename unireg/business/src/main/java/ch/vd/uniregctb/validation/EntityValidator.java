package ch.vd.uniregctb.validation;

import ch.vd.registre.base.validation.ValidationResults;

/**
 * Interface implémentée par un validateur capable de valider une entité de classe T
 */
public interface EntityValidator<T> {

	/**
	 * Valide l'entité donnée et renvoie les éventuelles erreurs / warnings rencontrés
	 * @param entity entité à valider
	 * @return résultat de la validation de l'entité
	 */
	ValidationResults validate(T entity);
}
