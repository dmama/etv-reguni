package ch.vd.uniregctb.declaration;

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

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_D16p0C4BEd2H4bonmeBdag"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_D16p0C4BEd2H4bonmeBdag"
 */
@Entity
@Table(name = "MODELE_FEUILLE_DOC")
public class ModeleFeuilleDocument extends HibernateEntity {

	private static final long serialVersionUID = -2048630558946131693L;

	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mbln4C4CEd2H4bonmeBdag"
	 */
	private String intituleFeuille;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3RQkgi4CEd2H4bonmeBdag"
	 */
	private ModeleDocument modeleDocument;

	/**
	 * Index qui permet d'ordonner les feuilles pour un modèle donné (SIFISC-2066).
	 */
	private Integer index;

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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the intituleFeuille
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mbln4C4CEd2H4bonmeBdag?GETTER"
	 */
	@Column(name = "INTITULE_FEUILLE")
	public String getIntituleFeuille() {
		// begin-user-code
		return intituleFeuille;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theIntituleFeuille the intituleFeuille to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mbln4C4CEd2H4bonmeBdag?SETTER"
	 */
	public void setIntituleFeuille(String theIntituleFeuille) {
		// begin-user-code
		intituleFeuille = theIntituleFeuille;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vqh4gOCPEd2HTeC2f-Vvpg"
	 */
	private String numeroFormulaire;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroFormulaire
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vqh4gOCPEd2HTeC2f-Vvpg?GETTER"
	 */
	@Column(name = "NO_FORMULAIRE")
	public String getNumeroFormulaire() {
		// begin-user-code
		return numeroFormulaire;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroFormulaire the numeroFormulaire to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vqh4gOCPEd2HTeC2f-Vvpg?SETTER"
	 */
	public void setNumeroFormulaire(String theNumeroFormulaire) {
		// begin-user-code
		numeroFormulaire = theNumeroFormulaire;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the modeleDocument
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3RQkgi4CEd2H4bonmeBdag?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "MODELE_ID", insertable = false, updatable = false)
	public ModeleDocument getModeleDocument() {
		// begin-user-code
		return modeleDocument;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theModeleDocument the modeleDocument to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3RQkgi4CEd2H4bonmeBdag?SETTER"
	 */
	public void setModeleDocument(ModeleDocument theModeleDocument) {
		// begin-user-code
		modeleDocument = theModeleDocument;
		// end-user-code
	}

	@Column(name = "SORT_INDEX")
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}
}
