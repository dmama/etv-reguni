package ch.vd.unireg.evenement.common;

import ch.vd.unireg.type.TypeEvenementErreur;

/**
 * Interface commune à toutes les erreurs des événements civils et entreprise
 */
public interface EvenementErreur {

	/**
	 * @return Description textuelle de l'erreur
	 */
	String getMessage();

	/**
	 * @return Callstack au moment de l'erreur
	 */
	String getCallstack();

	/**
	 * @return Type d'erreur
	 */
	TypeEvenementErreur getType();
}
