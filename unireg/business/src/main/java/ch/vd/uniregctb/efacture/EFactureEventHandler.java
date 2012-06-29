package ch.vd.uniregctb.efacture;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.interfaces.efacture.data.DemandeBrute;

/**
 * Interface pour les handlers des événements reçus de la part de l'application e-facture
 */
public interface EFactureEventHandler {

	/**
	 * Appelé à la réception d'un événement e-facture
	 * @param event l'événement reçu
	 */
	void handle(DemandeBrute event) throws Exception;

	/**
	 * @return Les resources utiles au parsing du fichier XML reçu
	 */
	ClassPathResource getRequestXSD();

}
