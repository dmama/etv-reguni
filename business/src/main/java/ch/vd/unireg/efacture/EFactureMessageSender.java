package ch.vd.unireg.efacture;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;

/**
 * Interface du service bas-niveau d'envoi de messages à l'application e-facture
 */
public interface EFactureMessageSender {

	/**
	 * Envoie un message à la e-facture qui annonce le refus d'une demande d'inscription
	 *
	 * @param idDemande     identifiant de la demande d'inscription refusée
	 * @param description   texte libre
	 * @param retourAttendu <code>true</code> si on demande une réponse (ACK) à l'application e-facture correspondant de cette demande
	 * @throws EFactureException en cas de problème
	 */
	String envoieRefusDemandeInscription(String idDemande, String description, boolean retourAttendu) throws EFactureException;

	/**
	 * Envoie un message à la e-facture qui annonce la mise en attente d'une demande d'inscription
	 *
	 * @param idDemande           identifiant de la demande d'inscription mise en attente
	 * @param typeAttenteEFacture Permet de determiner le type de message à envoyer: Attente de contact ou attente de confirmation
	 * @param description         description de l'attente
	 * @param idArchivage         clé d'archivage générée lors du traitement des documents E-Facture
	 * @param retourAttendu       <code>true</code> si on demande une réponse (ACK) à l'application e-facture correspondant de cette demande
	 * @throws EFactureException en cas de problème
	 */
	String envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EFactureException;

	/**
	 * Envoie un message à la e-facture qui annonce l'acceptation d'une demande d'inscription
	 *
	 * @param idDemande     identifiant de la demande d'inscription acceptée
	 * @param retourAttendu <code>true</code> si on demande une réponse (ACK) à l'application e-facture correspondant de cette demande
	 * @param description   texte libre
	 * @throws EFactureException en cas de problème
	 */
	String envoieAcceptationDemandeInscription(String idDemande, boolean retourAttendu, String description) throws EFactureException;

	/**
	 * Envoie un message à la e-facture qui annonce la suspension du contribuable
	 *
	 * @param noCtb         numéro du contribuable dont les activités e-facture doivent être suspendues
	 * @param retourAttendu <code>true</code> si on demande une réponse (ACK) à l'application e-facture correspondant de cette demande
	 * @param description   texte libre
	 * @throws EFactureException en cas de problème
	 */
	String envoieSuspensionContribuable(long noCtb, boolean retourAttendu, String description) throws EFactureException;

	/**
	 * Envoie un message à la e-facture qui annonce l'activation du contribuable
	 *
	 * @param noCtb         numéro du contribuable dont les activités e-facture peuvent être activées
	 * @param retourAttendu <code>true</code> si on demande une réponse (ACK) à l'application e-facture correspondant de cette demande
	 * @param description   texte libre
	 * @throws EFactureException en cas de problème
	 */
	String envoieActivationContribuable(long noCtb, boolean retourAttendu, String description) throws EFactureException;

	/**
	 * Envoie un message à la e-facture qui demande le changement d'adresse mail associée au contribuable
	 *
	 * @param noCtb         numéro du contribuable concerné
	 * @param newMail       nouvelle valeur de l'adresse mail
	 * @param retourAttendu <code>true</code> si on demande une réponse (ACK) à l'application e-facture correspondant de cette demande
	 * @param description   description associée au changement d'adresse mail
	 * @return le business id du message de demande envoyé à l'application e-facture
	 * @throws EFactureException
	 */
	String envoieDemandeChangementEmail(long noCtb, @Nullable String newMail, boolean retourAttendu, String description) throws EFactureException;

	/**
	 * Envoie une demande de désinscription / abandon des demandes précédentes dans le cadre du traitement d'une nouvelle demande
	 *
	 * @param noCtb             numéro du contribuable concerné
	 * @param idNouvelleDemande identifiant de la demande en cours de traitement
	 * @param description       texte libre
	 * @throws EFactureException
	 */
	void demandeDesinscriptionContribuable(long noCtb, String idNouvelleDemande, String description) throws EFactureException;

}
