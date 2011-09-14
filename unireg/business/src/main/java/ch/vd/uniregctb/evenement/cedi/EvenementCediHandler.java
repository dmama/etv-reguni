package ch.vd.uniregctb.evenement.cedi;

import java.util.Map;

/**
 * Interface de callback pour traiter les événements du CEDI.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementCediHandler {

	/**
	 * Traite l'événement CEDI spécifié.
	 *
	 * @param event un événement CEDI (non-persisté).
	 * @throws EvenementCediException en cas d'erreur métieur dans le traitement de l'événement.
	 */
	public void onEvent(EvenementCedi event, Map<String, String> incomingHeaders) throws EvenementCediException;
}
