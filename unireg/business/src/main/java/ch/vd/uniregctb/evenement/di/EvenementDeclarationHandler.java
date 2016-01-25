package ch.vd.uniregctb.evenement.di;


import java.util.Map;

import org.springframework.core.io.ClassPathResource;

/**
 * Interface de callback pour traiter les événements Declaration.
 *
 * @author Baba NGOM
 */
public interface EvenementDeclarationHandler {

	/**
	 * Traite l'événement Declaration spécifié.
	 *
	 * @param event un événement Declaration (non-persisté).
	 * @param incomingHeaders headers custom de l'événement de quittancement qui vient de nous parvenir
	 * @throws EvenementDeclarationException en cas d'erreur métieur dans le traitement de l'événement.
	 */
	public void onEvent(EvenementDeclaration event, Map<String, String> incomingHeaders) throws EvenementDeclarationException;

	public ClassPathResource getRequestXSD();
}
