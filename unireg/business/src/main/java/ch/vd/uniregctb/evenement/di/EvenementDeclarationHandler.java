package ch.vd.uniregctb.evenement.di;


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
	 * @throws ch.vd.uniregctb.evenement.cedi.EvenementCediException en cas d'erreur métieur dans le traitement de l'événement.
	 */
	public void onEvent(EvenementDeclaration event) throws EvenementDeclarationException;

	public ClassPathResource getRequestXSD();
}
