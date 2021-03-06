package ch.vd.unireg.evenement.iam;


/**
 * Interface de callback pour traiter les événements provenant du portail IAM.
 *
 * @author Baba NGOM <baba-issa.ngom@vd.ch>
 */
public interface EvenementIAMHandler {

	/**
	 * Traite l'événement IAM spécifié.
	 *
	 * @param event un événement IAM (non-persisté).
	 * @throws EvenementIAMException en cas d'erreur métieur dans le traitement de l'événement.
	 */
	void onEvent(EvenementIAM event) throws EvenementIAMException;
}
