package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import org.hibernate.annotations.Type;

/**
 * Contient les données d'une requête d'identification d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Embeddable
public class Demande {

	public enum PrioriteEmetteur {
		NON_PRIORITAIRE, PRIORITAIRE
	}

	/**
	 * Date d'arrivée de la demande
	 */
	private Date date;

	/**
	 * Chaque demande d’identification faite par un des systèmes utilisateurs, est identifiée de manière unique par un identifiant de ce
	 * système utilisateur. Le message transitant par l’ESB, l’identifiant du message doit respecter les règles de formatage de celui-ci.
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
	 * Le système demandeur (utilisateur – par exemple ACICOM) fournit une priorité qui est propre à son besoin. La priorité varie de 0 à 9,
	 * 0 étant la plus haute priorité.
	 */
	private int prioriteUtilisateur;

	/**
	 * Informations sur le contribuable à rechercher
	 */
	private CriteresPersonne personne;

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

	public void setPrioriteEmetteur(PrioriteEmetteur prioriteEmetteur) {
		this.prioriteEmetteur = prioriteEmetteur;
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
}
