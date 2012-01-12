package ch.vd.uniregctb.inbox;

/**
 * Interface implémentée par les entités qui veulent être notifiées
 * de la création d'une nouvelle inbox
 */
public interface InboxManagementListener {

	/**
	 * Méthode appelée à chaque création de nouvelle inbox
	 * @param visa visa de l'utilisateur dont l'inbox vient d'être créée
	 */
	void onNewInbox(String visa);
}
