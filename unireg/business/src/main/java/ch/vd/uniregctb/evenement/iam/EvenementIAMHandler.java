package ch.vd.uniregctb.evenement.iam;


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
	 * @throws ch.vd.uniregctb.evenement.cedi.EvenementIAMException en cas d'erreur métieur dans le traitement de l'événement.
	 */
	public void onEvent(EvenementIAM event) throws EvenementIAMException;
}
