package ch.vd.unireg.efacture;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.efacture.data.Demande;

/**
 * Interface pour les handlers des événements reçus de la part de l'application e-facture
 */
public interface EFactureEventHandler {

	/**
	 * Appelé à la réception d'un événement e-facture
	 * @param event l'événement reçu
	 */
	void handle(Demande event) throws Exception;

	/**
	 * @return Les resources utiles au parsing du fichier XML reçu
	 */
	@NotNull
	List<String> getRequestXSDs();
}