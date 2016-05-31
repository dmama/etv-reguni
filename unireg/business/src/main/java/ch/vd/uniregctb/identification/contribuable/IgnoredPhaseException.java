package ch.vd.uniregctb.identification.contribuable;

/**
 * Exception lancée par la mécanique à plusieurs phases de l'algorithme d'identification de contribuable quand le NAVS13 est inconnu
 * afin de signifier que la phase courante doit être ignorée
 */
public class IgnoredPhaseException extends Exception {

	public IgnoredPhaseException(String message) {
		super(message);
	}

	public IgnoredPhaseException(String message, Throwable cause) {
		super(message, cause);
	}
}
