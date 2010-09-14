package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.LengthConstants;

/**
 * Contient les données d'une requête d'identification d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Embeddable
public class Demande {

	public enum PrioriteEmetteur {
		NON_PRIORITAIRE,
		PRIORITAIRE
	}

	public enum ModeIdentificationType {
		SANS_MANUEL,
		MANUEL_AVEC_ACK,
		MANUEL_SANS_ACK
	}

	/**
	 * Date d'arrivée de la demande
	 */
	private Date date;

	/**
	 * Chaque demande d’identification faite par un des systèmes utilisateurs, est identifiée de manière unique par un identifiant de ce système utilisateur. Le message transitant par l’ESB,
	 * l’identifiant du message doit respecter les règles de formatage de celui-ci.
	 */
	private String messageId;

	/**
	 * Identifiant de l’émetteur de la demande. Il s’agit d’un intervenant extérieur.
	 */
	private String emetteurId;

	/**
	 * La demande concerne un certain type de besoin métier.
	 */
	private String typeMessage;

	/**
	 * Une demande est dépendante d’une période fiscale définie par une année sur 4 chiffres. Ex : 2009.
	 */
	private int periodeFiscale;

	/**
	 * L’émetteur fournit une priorité qui est propre à son besoin. La priorité est binaire : message prioritaire ou non.
	 */
	private PrioriteEmetteur prioriteEmetteur;


	/**
	 * SANS_MANUEL: le demandeur ne souhaite pas que sa demande passe en mode manuel si le mode automatique échoue. La réponse sera immédiatement négative, sans autre traitement.
	 * <p/>
	 * MANUEL_AVEC_ACK: le demandeur souhaite que sa demande passe en mode manuel si le mode automatique échoue. Cependant, dés l'échec du mode automtique, une sorte d'accusé de réception sera envoyée au
	 * demandeur avant que le mode manuel n'aboutisse.
	 * <p/>
	 * MANUEL_SANS_ACK: le demandeur souhaite que sa demande passe en mode manuel si le mode automatique échoue. Il ne recevra rien tant que le mode manuel n'aura aboutie.
	 */
	private ModeIdentificationType modeIdentification;

	/**
	 * Le système demandeur (utilisateur – par exemple ACICOM) fournit une priorité qui est propre à son besoin. La priorité varie de 0 à 9, 0 étant la plus haute priorité.
	 */
	private int prioriteUtilisateur;

	/**
	 * Informations sur le contribuable à rechercher
	 */
	private CriteresPersonne personne;

	/**
	 * Type de la demande: MeldeWesen, NCS, ...
	 */
	private TypeDemande typeDemande;
	

	@Column(name = "DATE_DEMANDE")
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Column(name = "MESSAGE_ID", length = 36, nullable = false)
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Column(name = "EMETTEUR_ID", length = 50, nullable = false)
	public String getEmetteurId() {
		return emetteurId;
	}

	public void setEmetteurId(String emetteurId) {
		this.emetteurId = emetteurId;
	}

	@Column(name = "TYPE_MESSAGE", length = 20, nullable = false)
	public String getTypeMessage() {
		return typeMessage;
	}

	public void setTypeMessage(String typeMessage) {
		this.typeMessage = typeMessage;
	}

	@Column(name = "PERIODE_FISCALE", nullable = false)
	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	@Column(name = "PRIO_EMETTEUR", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.TypePrioriteEmetteurUserType")
	public PrioriteEmetteur getPrioriteEmetteur() {
		return prioriteEmetteur;
	}

	@Column(name = "DEMANDE_TYPE", length = LengthConstants.IDENT_DEMANDE_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeDemandeUserType")
	public TypeDemande getTypeDemande() {
		return typeDemande;
	}

	public void setPrioriteEmetteur(PrioriteEmetteur prioriteEmetteur) {
		this.prioriteEmetteur = prioriteEmetteur;
	}

	@Column(name = "MODE_IDENTIFICATION", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.ModeIdentificationTypeUserType")
	public ModeIdentificationType getModeIdentification() {
		return modeIdentification;
	}

	public void setModeIdentification(ModeIdentificationType modeIdentification) {
		this.modeIdentification = modeIdentification;
	}


	@Column(name = "PRIO_UTILISATEUR", nullable = false)
	public int getPrioriteUtilisateur() {
		return prioriteUtilisateur;
	}

	public void setPrioriteUtilisateur(int prioriteUtilisateur) {
		this.prioriteUtilisateur = prioriteUtilisateur;
	}

	@Embedded
	public CriteresPersonne getPersonne() {
		return personne;
	}

	public void setPersonne(CriteresPersonne personne) {
		this.personne = personne;
	}

	public void setTypeDemande(TypeDemande typeDemande) {
		this.typeDemande = typeDemande;
	}
}
