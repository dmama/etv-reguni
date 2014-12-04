package ch.vd.uniregctb.identification.contribuable;

/**
 * Exception envoyée par le {@link IdentificationContribuableHelper} dans les méthodes où on demande de remplacer les ae, oe, et ue
 * par des a, o, u si aucun de ces phonèmes n'est présent à la base
 */
public class NoEsToRemoveException extends IgnoredPhaseException {

	public NoEsToRemoveException(String message) {
		super(message);
	}
}
