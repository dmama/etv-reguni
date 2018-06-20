package ch.vd.unireg.evenement.entreprise;

/**
 * Interface utilisée pour récupérer l'information du nombre d'événements entreprise
 * sauvegardés en base (= non ignorés) afin de la publier, par exemple, via JMX
 */
public interface EvenementEntrepriseReceptionMonitor {

	/**
	 * @return le nombre d'événements entreprise insérés en base depuis le démarrage de l'application
	 */
	int getNombreEvenementsNonIgnores();

	/**
	 * Méthode utilisée dans les tests "live" pour re-demander le traitement de la queue d'événements de l'entreprise donné
	 * @param noEntrepriseCivile identifiant de l'individu dont on veut relancer le traitement
	 * @param mode traitement batch ou manuel
	 */
	void demanderTraitementQueue(long noEntrepriseCivile, EvenementEntrepriseProcessingMode mode);

	/**
	 * @return le nombre d'entreprises actuellement en attente de traitement de ses événements
	 */
	int getNombreEntreprisesEnAttenteDeTraitement();

	int getNombreEntreprisesEnAttenteDansLaQueueImmediate();

	int getNombreEntreprisesEnAttenteDansLaQueueBatch();

	int getNombreEntreprisesEnAttenteDansLaQueuePrioritaire();

	int getNombreEntreprisesEnTransitionVersLaQueueFinale();

	int getNombreEntreprisesEnAttenteDansLaQueueFinale();

	Long getMoyenneGlissanteDureeAttenteDansLaQueueImmediate();

	Long getMoyenneGlissanteDureeAttenteDansLaQueueBatch();

	Long getMoyenneGlissanteDureeAttenteDansLaQueuePrioritaire();

	Long getMoyenneTotaleDureeAttenteDansLaQueueImmediate();

	Long getMoyenneTotaleDureeAttenteDansLaQueueBatch();

	Long getMoyenneTotaleDureeAttenteDansLaQueuePrioritaire();
}
