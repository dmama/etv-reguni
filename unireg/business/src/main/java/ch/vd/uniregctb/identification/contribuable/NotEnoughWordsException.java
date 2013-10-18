package ch.vd.uniregctb.identification.contribuable;

/**
 * Exception envoyée par le {@link IdentificationContribuableHelper} dans les méthodes où on demande d'enlever le dernier mot d'un champ
 * si ce champ contient moins de deux mots à la base
 */
public class NotEnoughWordsException extends IgnoredPhaseException {

	public NotEnoughWordsException(String message) {
		super(message);
	}
}
