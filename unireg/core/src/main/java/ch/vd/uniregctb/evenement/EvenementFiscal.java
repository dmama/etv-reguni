package ch.vd.uniregctb.evenement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc --> Changement intervenu sur un tiers, induit ou non par un changement de l'individu et
 * suscptible d'intéresser une oui plusieurs applications fiscales.
 *
 * @uml.annotations derived_abstraction="platform:/resource/unireg-modele/04Unireg%20-%20data%20model%20tiers.emx#_nLi8sVx9Edygsbnw9h5bVw"
 * @generated "sourceid:platform:/resource/unireg-modele/04Unireg%20-%20data%20model%20tiers.emx#_nLi8sVx9Edygsbnw9h5bVw"
 */
@Entity
@Table(name = "EVENEMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "EVT_TYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("EvenementFiscal")
public class EvenementFiscal extends HibernateEntity {

	/**
	 *
	 */
	private static final long serialVersionUID = 7593360403141492380L;

	private Long id;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tFx9Edygsbnw9h5bVw"
	 */
	private RegDate dateEvenement;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tlx9Edygsbnw9h5bVw"
	 */
	private Tiers tiers;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8s1x9Edygsbnw9h5bVw"
	 */
	private TypeEvenementFiscal type;

	private Long numeroTechnique;

	@Column(name = "NUMERO_TECHNIQUE")
	public Long getNumeroTechnique() {
		return numeroTechnique;
	}

	public void setNumeroTechnique(Long numeroTechnique) {
		this.numeroTechnique = numeroTechnique;
	}

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
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the type
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8s1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "TYPE", length = LengthConstants.EVTFISCAL_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeEvenementFiscalUserType")
	public TypeEvenementFiscal getType() {
		// begin-user-code
		return type;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theType
	 *            the type to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8s1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setType(TypeEvenementFiscal theType) {
		// begin-user-code
		type = theType;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the dateEvenement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tFx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "DATE_EVENEMENT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEvenement() {
		// begin-user-code
		return dateEvenement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theDateEvenement
	 *            the dateEvenement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tFx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setDateEvenement(RegDate theDateEvenement) {
		// begin-user-code
		dateEvenement = theDateEvenement;
		// end-user-code
	}


	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the tiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tlx9Edygsbnw9h5bVw?GETTER"
	 */
	@JoinColumn(name = "TIERS_ID")
	@ManyToOne(fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_EV_FSC_TRS_ID")
	public Tiers getTiers() {
		// begin-user-code
		return tiers;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param tiers
	 *            the tiers to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8tlx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setTiers(Tiers tiers) {
		// begin-user-code
		this.tiers = tiers;
		// end-user-code
	}

}
