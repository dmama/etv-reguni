package ch.vd.uniregctb.evenement.organisation;

/**
 * Interface du composant qui est capable, comme par exemple au démarrage de l'application,
 * de récupérer les numéros d'organisation sur les événements organisation "à traiter" afin de les
 * insérer proprement dans la mécanique de traitement
 */
public interface EvenementOrganisationRecuperateur {

	/**
	 * Récupère les événements organisation "à traiter" (assignation du numéro d'individu)
	 */
	void recupererEvenementsOrganisation();
}
