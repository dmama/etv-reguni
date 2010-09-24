package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.CategorieIdentifiant;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * Identification d'une personne physique dans un registre fédéral (RCE, InfoStar...), cantonal, communal ou autre, à l'exclusion du nouveau numéro d'assuré social.
 * Voir norme eCH-0044.
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zwFbAFyUEdyz_5BS6IxMlQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zwFbAFyUEdyz_5BS6IxMlQ"
 */
@Entity
@Table(name = "IDENTIFICATION_PERSONNE")
public class IdentificationPersonne extends HibernateEntity implements LinkedEntity {

	private static final long serialVersionUID = -2912407089876896897L;

	private Long id;

	private PersonnePhysique personnePhysique;

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

	public void setId(Long id) {
		this.id = id;
	}


	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "NON_HABITANT_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_ID_PERS_TIERS_ID", columnNames = "NON_HABITANT_ID")
	public PersonnePhysique getPersonnePhysique() {
		return personnePhysique;
	}

	public void setPersonnePhysique(PersonnePhysique PersonnePhysique) {
		this.personnePhysique = PersonnePhysique;
	}


	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Système fédéral, cantonal, communal ou autre ayant attribué l'identifiant
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zwFbAlyUEdyz_5BS6IxMlQ"
	 */
	private CategorieIdentifiant categorieIdentifiant;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the source
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zwFbAlyUEdyz_5BS6IxMlQ?GETTER"
	 */
	@Column(name = "CATEGORIE", length = LengthConstants.IDENTPERSONNE_CATEGORIE)
	@Type(type = "ch.vd.uniregctb.hibernate.CategorieIdentifiantUserType")
	public CategorieIdentifiant getCategorieIdentifiant() {
		// begin-user-code
		return categorieIdentifiant;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theSource the source to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zwFbAlyUEdyz_5BS6IxMlQ?SETTER"
	 */
	public void setCategorieIdentifiant(CategorieIdentifiant theSource) {
		// begin-user-code
		categorieIdentifiant = theSource;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Identifiant de la personne physique dans le système source
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zwFbBFyUEdyz_5BS6IxMlQ"
	 */
	private String identifiant;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the identifiant
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zwFbBFyUEdyz_5BS6IxMlQ?GETTER"
	 */
	@Column(name = "IDENTIFIANT", length = LengthConstants.IDENTPERSONNE_IDENTIFIANT)
	public String getIdentifiant() {
		// begin-user-code
		return identifiant;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theIdentifiant the identifiant to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zwFbBFyUEdyz_5BS6IxMlQ?SETTER"
	 */
	public void setIdentifiant(String theIdentifiant) {
		// begin-user-code
		identifiant = theIdentifiant;
		// end-user-code
	}

	@Transient
	public List<?> getLinkedEntities() {
		return personnePhysique == null ? null : Arrays.asList(personnePhysique);
	}
}
