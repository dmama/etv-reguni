package ch.vd.uniregctb.validation;

import ch.vd.registre.base.validation.ValidationResults;

/**
 * Interface implémentée par le service de validation d'entités
 */
public interface ValidationService {

	/**
	 * Enregistre un nouveau validateur dans le service de validation
	 * @param clazz classe pour laquelle ce validateur est utilisable
	 * @param validator validateur à enregistrer
	 */
	<T> void registerValidator(Class<T> clazz, EntityValidator<T> validator);

	/**
	 * Dés-enregistre un validateur du service de validation
	 * @param clazz classe pour laquelle ce validateur est utilisable
	 * @param validator validateur à dés-enregistrer
	 */
	<T> void unregisterValidator(Class<T> clazz, EntityValidator<T> validator);

	/**
	 * Effectue la validation de l'objet passé en paramètre et retourne un rapport de validation
	 * @param object objet à valider
	 * @return rapport de validation contenant les éventuelles erreurs et warnings rencontrés
	 */
	ValidationResults validate(Object object);

	/**
	 * @return <b>vrai</b> si le service est entrain de valider un objet dans le thread courant; <b>faux</b> autrement.
	 */
	boolean isInValidation();

}
