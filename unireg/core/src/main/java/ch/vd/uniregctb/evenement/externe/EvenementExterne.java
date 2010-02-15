package ch.vd.uniregctb.evenement.externe;

import java.util.Date;

import javax.persistence.*;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Tiers;

/**
 *
 * @author xcicfh
 */
@Entity
@Table(name = "EVENEMENT_EXTERNE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "EVENT_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class EvenementExterne extends HibernateEntity {

	private static final long serialVersionUID = -2680733628961424191L;

	private Long id;
	protected Tiers tiers;
	private Date dateEvenement;
	private Date dateTraitement;
	private String message;
	private String errorMessage;
	private String correlationId;
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
	@Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the dateEvenement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tFx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "DATE_EVENEMENT")
	@Temporal(TemporalType.DATE)
	public Date getDateEvenement() {
		return dateEvenement;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theDateEvenement
	 *            the dateEvenement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tFx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setDateEvenement(Date theDateEvenement) {
		dateEvenement = theDateEvenement;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the dateTraitement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "DATE_TRAITEMENT",nullable=true)
	@Temporal(TemporalType.DATE)
	public Date getDateTraitement() {
		return dateTraitement;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theDateTraitement
	 *            the dateTraitement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setDateTraitement(Date theDateTraitement) {
		// begin-user-code
		dateTraitement = theDateTraitement;
		// end-user-code
	}

	/**
	 * @return the etat
	 */
	@Column(name = "ETAT", length = LengthConstants.EVTEXTERNE_ETAT)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeEtatEvenementUserType")
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
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		if (errorMessage != null && errorMessage.length() > 255) {
			throw new IllegalArgumentException("Le message d'erreur <" + errorMessage + "> contient plus de 255 caractères.");
		}
		this.errorMessage = errorMessage;
	}

	/**
	 * @return the correlationID
	 */
	@Column(name = "CORRELATION_ID", nullable=false)
	public String getCorrelationId() {
		return correlationId;
	}

	/**
	 * @param correlationID the correlationID to set
	 */
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
}
