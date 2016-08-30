package ch.vd.uniregctb.evenement.organisation;

/**
 * Interface utilisée pour récupérer l'information du nombre d'événements organisation
 * sauvegardés en base (= non ignorés) afin de la publier, par exemple, via JMX
 */
public interface EvenementOrganisationReceptionMonitor {

	/**
	 * @return le nombre d'événements organisation insérés en base depuis le démarrage de l'application
	 */
	int getNombreEvenementsNonIgnores();

	/**
	 * Méthode utilisée dans les tests "live" pour re-demander le traitement de la queue d'événements de l'organisation donné
	 * @param noOrganisation identifiant de l'individu dont on veut relancer le traitement
	 * @param mode traitement batch ou manuel
	 */
	void demanderTraitementQueue(long noOrganisation, EvenementOrganisationProcessingMode mode);

	/**
	 * @return le nombre d'organisations actuellement en attente de traitement de ses événements
	 */
	int getNombreOrganisationsEnAttenteDeTraitement();

	int getNombreOrganisationsEnAttenteDansLaQueueImmediate();

	int getNombreOrganisationsEnAttenteDansLaQueueBatch();

	int getNombreOrganisationsEnAttenteDansLaQueuePrioritaire();

	int getNombreOrganisationsEnTransitionVersLaQueueFinale();

	int getNombreOrganisationsEnAttenteDansLaQueueFinale();

	Long getMoyenneGlissanteDureeAttenteDansLaQueueImmediate();

	Long getMoyenneGlissanteDureeAttenteDansLaQueueBatch();

	Long getMoyenneGlissanteDureeAttenteDansLaQueuePrioritaire();

	Long getMoyenneTotaleDureeAttenteDansLaQueueImmediate();

	Long getMoyenneTotaleDureeAttenteDansLaQueueBatch();

	Long getMoyenneTotaleDureeAttenteDansLaQueuePrioritaire();
}
