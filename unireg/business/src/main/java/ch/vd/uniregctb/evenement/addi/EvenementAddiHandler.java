package ch.vd.uniregctb.evenement.addi;


import org.springframework.core.io.ClassPathResource;

/**
 * Interface de callback pour traiter les événements ADDI.
 *
 * @author Baba NGOM
 */
public interface EvenementAddiHandler {

	/**
	 * Traite l'événement ADDI spécifié.
	 *
	 * @param event un événement ADDI (non-persisté).
	 * @throws ch.vd.uniregctb.evenement.cedi.EvenementCediException en cas d'erreur métieur dans le traitement de l'événement.
	 */
	public void onEvent(EvenementAddi event) throws EvenementAddiException;

	public ClassPathResource getRequestXSD();
}
