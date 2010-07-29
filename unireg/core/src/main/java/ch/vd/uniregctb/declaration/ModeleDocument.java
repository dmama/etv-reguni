package ch.vd.uniregctb.declaration;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zS71IC3_Ed2H4bonmeBdag"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zS71IC3_Ed2H4bonmeBdag"
 */
@Entity
@Table(name = "MODELE_DOCUMENT")
public class ModeleDocument extends HibernateEntity {

	private static final long serialVersionUID = -8958189002571119165L;

	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3RQkgS4CEd2H4bonmeBdag"
	 */
	private Set<ModeleFeuilleDocument> modelesFeuilleDocument;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_70VOcC3_Ed2H4bonmeBdag"
	 */
	private TypeDocument typeDocument;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_1RrkUi4AEd2H4bonmeBdag"
	 */
	private PeriodeFiscale periodeFiscale;

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
	 * @return the typeDocument
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_70VOcC3_Ed2H4bonmeBdag?GETTER"
	 */
	@Column(name = "TYPE_DOCUMENT", length = LengthConstants.MODELEDOC_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeDocumentUserType")
	public TypeDocument getTypeDocument() {
		// begin-user-code
		return typeDocument;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTypeDocument the typeDocument to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_70VOcC3_Ed2H4bonmeBdag?SETTER"
	 */
	public void setTypeDocument(TypeDocument theTypeDocument) {
		// begin-user-code
		typeDocument = theTypeDocument;
		// end-user-code
	}

	/**
	 * Ajoute le modèle spécifié à la période fiscale.
	 */
	public boolean addModeleFeuilleDocument(ModeleFeuilleDocument feuille) {
		if (modelesFeuilleDocument == null) {
			modelesFeuilleDocument = new HashSet<ModeleFeuilleDocument>();
		}
		return modelesFeuilleDocument.add(feuille);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the modelesFeuilleDocument
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3RQkgS4CEd2H4bonmeBdag?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "MODELE_ID")
	@ForeignKey(name = "FK_FLLE_MODOC_ID")
	public Set<ModeleFeuilleDocument> getModelesFeuilleDocument() {
		// begin-user-code
		return modelesFeuilleDocument;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theModelesFeuilleDocument the modelesFeuilleDocument to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3RQkgS4CEd2H4bonmeBdag?SETTER"
	 */
	public void setModelesFeuilleDocument(Set<ModeleFeuilleDocument> theModelesFeuilleDocument) {
		// begin-user-code
		modelesFeuilleDocument = theModelesFeuilleDocument;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the periodeFiscale
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_1RrkUi4AEd2H4bonmeBdag?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "PERIODE_ID", insertable = false, updatable = false)
	public PeriodeFiscale getPeriodeFiscale() {
		// begin-user-code
		return periodeFiscale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param thePeriodeFiscale the periodeFiscale to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_1RrkUi4AEd2H4bonmeBdag?SETTER"
	 */
	public void setPeriodeFiscale(PeriodeFiscale thePeriodeFiscale) {
		// begin-user-code
		periodeFiscale = thePeriodeFiscale;
		// end-user-code
	}
}
