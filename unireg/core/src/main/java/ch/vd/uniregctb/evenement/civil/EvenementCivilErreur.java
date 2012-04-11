package ch.vd.uniregctb.evenement.civil;

import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * Interface commune à toutes les erreurs des événements civils
 */
public interface EvenementCivilErreur {

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
