package ch.vd.uniregctb.efacture;

/**
 * Interface du service bas-niveau d'envoi de messages à l'application e-facture
 */
public interface EFactureMessageSender {

	/**
	 * Envoie un message à la e-facture qui annonce le refus d'une demande d'inscription
	 *
	 * @param idDemande identifiant de la demande d'inscription refusée
	 * @param typeRefusEFacture
	 * @throws EvenementEfactureException en cas de problème
	 */
	void envoieRefusDemandeInscription(String idDemande, TypeRefusEFacture typeRefusEFacture) throws EvenementEfactureException;

	/**
	 * Envoie un message à la e-facture qui annonce la mise en attente d'une demande d'inscription
	 *
	 * @param idDemande identifiant de la demande d'inscription mise en attente
	 * @param typeAttenteEFacture
	 * @throws EvenementEfactureException en cas de problème
	 */
	void envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteEFacture typeAttenteEFacture) throws EvenementEfactureException;

	/**
	 * Envoie un message à la e-facture qui annonce l'acceptation d'une demande d'inscription
	 * @param idDemande identifiant de la demande d'inscription acceptée
	 * @throws EvenementEfactureException en cas de problème
	 */
	void envoieAcceptationDemandeInscription(String idDemande) throws EvenementEfactureException;

	/**
	 * Envoie un message à la e-facture qui annonce la suspension du contribuable
	 * @param noCtb numéro du contribuable dont les activités e-facture doivent être suspendues
	 * @throws EvenementEfactureException en cas de problème
	 */
	void envoieSuspensionContribuable(long noCtb) throws EvenementEfactureException;

	/**
	 * Envoie un message à la e-facture qui annonce l'activation du contribuable
	 * @param noCtb numéro du contribuable dont les activités e-facture peuvent être activées
	 * @throws EvenementEfactureException en cas de problème
	 */
	void envoieActivationContribuable(long noCtb) throws EvenementEfactureException;

}
