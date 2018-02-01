package ch.vd.unireg.evenement.externe;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.util.Date;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.Tiers;

/**
 *
 * @author xcicfh
 */
@Entity
@Table(name = "EVENEMENT_EXTERNE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "EVENT_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class EvenementExterne extends HibernateEntity {

	private Long id;
	protected Tiers tiers;
	private Date dateEvenement;
	private Date dateTraitement;
	private String message;
	private String errorMessage;
	private String businessId;
	private EtatEvenementExterne etat;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long theId) {
		this.id = theId;
	}

	/**
	 * @return the tiers
	 */
	@JoinColumn(name = "TIERS_ID")
	@ManyToOne()
	@ForeignKey(name = "FK_EV_EXT_TRS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	/**
	 * @param tiers the tiers to set
	 */
	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	/**
	 * @return the message
	 */
	@Column(name = "MESSAGE")
	@Type(type = "ch.vd.unireg.hibernate.StringAsClobUserType")
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	@Column(name = "DATE_EVENEMENT")
	@Temporal(TemporalType.DATE)
	public Date getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(Date theDateEvenement) {
		dateEvenement = theDateEvenement;
	}

	@Column(name = "DATE_TRAITEMENT",nullable=true)
	@Temporal(TemporalType.DATE)
	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date theDateTraitement) {
		dateTraitement = theDateTraitement;
	}

	/**
	 * @return the etat
	 */
	@Column(name = "ETAT", length = LengthConstants.EVTEXTERNE_ETAT)
	@Type(type = "ch.vd.unireg.hibernate.TypeEtatEvenementUserType")
	public EtatEvenementExterne getEtat() {
		return etat;
	}

	/**
	 * @param etat the etat to set
	 */
	public void setEtat(EtatEvenementExterne etat) {
		this.etat = etat;
	}

	/**
	 * @return the errorMessage
	 */
	@Column(name = "ERREUR_MESSAGE", nullable=true)
	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(@Nullable String errorMessage) {
		if (errorMessage != null && errorMessage.length() > 255) {
			throw new IllegalArgumentException("Le message d'erreur <" + errorMessage + "> contient plus de 255 caractères.");
		}
		this.errorMessage = errorMessage;
	}

	/**
	 * @return the businessId
	 */
	@Column(name = "CORRELATION_ID", nullable=false) // nom colonne faux pour raisons historiques
	public String getBusinessId() {
		return businessId;
	}

	/**
	 * @param businessId the businessId to set
	 */
	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}
}
