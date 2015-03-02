package ch.vd.uniregctb.evenement.identification.contribuable;

/**
 * Interface de callback pour traiter les demandes d'identification de contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface DemandeHandler {

	/**
	 * La classe qui implémente cette méthode est responsable d'entreprendre toutes les actions <i>métier</i> nécessaires au traitement
	 * correct du message.
	 *
	 * @param message
	 *            le message à traiter
	 */
	void handleDemande(IdentificationContribuable message);
}
