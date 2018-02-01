package ch.vd.unireg.evenement.organisation;

/**
 * Exception lancée dans le cadre du traitement des événements, et qui correspond à un arrêt "volontaire" du traitement
 * et indique la volonté que les messages de traitement soient sauvegardés dans les erreurs accessibles à l'utilisateur,
 * en dépit du rollback de la transaction.
 */
public class EvenementOrganisationAbortException extends EvenementOrganisationException {
	public EvenementOrganisationAbortException(String message) {
		super(message);
	}
}
