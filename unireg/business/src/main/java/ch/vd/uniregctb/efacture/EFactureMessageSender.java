package ch.vd.uniregctb.efacture;

/**
 * Interface du service bas-niveau d'envoi de messages à l'application e-facture
 */
public interface EFactureMessageSender {

	/**
	 * Envoie un message à la e-facture qui annonce le refus d'une demande d'inscription
	 *
	 *
	 * @param idDemande identifiant de la demande d'inscription refusée
	 * @param typeRefusEFacture
	 * @param retourAttendu
	 * @throws EvenementEfactureException en cas de problème
	 */
	String envoieRefusDemandeInscription(String idDemande, TypeRefusEFacture typeRefusEFacture, boolean retourAttendu) throws EvenementEfactureException;

	/**
	 * Envoie un message à la e-facture qui annonce la mise en attente d'une demande d'inscription
	 *
	 *
	 *
	 *
	 *
	 * @param idDemande identifiant de la demande d'inscription mise en attente
	 * @param typeAttenteEFacture Permet de determiner le type de message à envoyer: Attente de contact ou attente de confirmation
	 * @param idArchivage clé d'archivage générée lors du traitement des documents E-Facture
	 * @param retourAttendu vrai si on attend une réponse de la E-facture, false sinon
	 * @throws EvenementEfactureException en cas de problème
	 */
	String envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteEFacture typeAttenteEFacture, String idArchivage, boolean retourAttendu) throws EvenementEfactureException;

	/**
	 * Envoie un message à la e-facture qui annonce l'acceptation d'une demande d'inscription
	 *
	 * @param idDemande identifiant de la demande d'inscription acceptée
	 * @param retourAttendu
	 * @throws EvenementEfactureException en cas de problème
	 */
	String envoieAcceptationDemandeInscription(String idDemande, boolean retourAttendu) throws EvenementEfactureException;

	/**
	 * Envoie un message à la e-facture qui annonce la suspension du contribuable
	 *
	 *
	 * @param noCtb numéro du contribuable dont les activités e-facture doivent être suspendues
	 * @param retourAttendu
	 * @throws EvenementEfactureException en cas de problème
	 */
	String envoieSuspensionContribuable(long noCtb, boolean retourAttendu) throws EvenementEfactureException;

	/**
	 * Envoie un message à la e-facture qui annonce l'activation du contribuable
	 *
	 * @param noCtb numéro du contribuable dont les activités e-facture peuvent être activées
	 * @param retourAttendu
	 * @throws EvenementEfactureException en cas de problème
	 */
	String envoieActivationContribuable(long noCtb, boolean retourAttendu) throws EvenementEfactureException;

}
