package ch.vd.unireg.evenement.retourdi.pp;

import java.util.Map;

public interface EvenementCediService {

	/**
	 * Traite l'événement CEDI spécifié.
	 *
	 * @param event un événement CEDI (non-persisté).
	 * @throws EvenementCediException en cas d'erreur métieur dans le traitement de l'événement.
	 */
	void onEvent(EvenementCedi event, Map<String, String> incomingHeaders) throws EvenementCediException;
}
