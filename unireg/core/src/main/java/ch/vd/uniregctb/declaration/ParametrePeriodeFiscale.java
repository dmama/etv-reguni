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

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeContribuable;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author xsifnr
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_9Us-8OCOEd2HTeC2f-Vvpg"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_9Us-8OCOEd2HTeC2f-Vvpg"
 */
@Entity
@Table(name = "PARAMETRE_PERIODE_FISCALE")
public class ParametrePeriodeFiscale extends HibernateEntity implements Duplicable<ParametrePeriodeFiscale> {

	private static final long serialVersionUID = 8321336807671103230L;

	private Long id;
	private TypeContribuable typeContribuable;
	private RegDate dateFinEnvoiMasseDI;
	private RegDate termeGeneralSommationReglementaire;
	private RegDate termeGeneralSommationEffectif;
	private PeriodeFiscale periodefiscale;

	public ParametrePeriodeFiscale() {
	}

	public ParametrePeriodeFiscale(ParametrePeriodeFiscale right) {
		this.typeContribuable = right.typeContribuable;
		this.dateFinEnvoiMasseDI = right.dateFinEnvoiMasseDI;
		this.termeGeneralSommationReglementaire = right.termeGeneralSommationReglementaire;
		this.termeGeneralSommationEffectif = right.termeGeneralSommationEffectif;
		this.periodefiscale = right.periodefiscale;
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

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the typeContribuable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_b0osIOCPEd2HTeC2f-Vvpg?GETTER"
	 */
	@Column(name = "TYPE_CTB", length = LengthConstants.DI_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeContribuableUserType")
	public TypeContribuable getTypeContribuable() {
		// begin-user-code
		return typeContribuable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTypeContribuable the typeContribuable to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_b0osIOCPEd2HTeC2f-Vvpg?SETTER"
	 */
	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		// begin-user-code
		typeContribuable = theTypeContribuable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateFinEnvoiMasseDI
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nyDxcFjJEd2uSoZKEkgcsw?GETTER"
	 */
	@Column(name = "DATE_FIN_ENVOI_MASSE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFinEnvoiMasseDI() {
		// begin-user-code
		return dateFinEnvoiMasseDI;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateFinEnvoiMasseDI the dateFinEnvoiMasseDI to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nyDxcFjJEd2uSoZKEkgcsw?SETTER"
	 */
	public void setDateFinEnvoiMasseDI(RegDate theDateFinEnvoiMasseDI) {
		// begin-user-code
		dateFinEnvoiMasseDI = theDateFinEnvoiMasseDI;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the termeGeneralSommationReglementaire
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_OTXloOqhEdySTq6PFlf9jQ?GETTER"
	 */
	@Column(name = "TERME_GEN_SOMM_REGL")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getTermeGeneralSommationReglementaire() {
		// begin-user-code
		return termeGeneralSommationReglementaire;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTermeGeneralSommationReglementaire the termeGeneralSommationReglementaire to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_OTXloOqhEdySTq6PFlf9jQ?SETTER"
	 */
	public void setTermeGeneralSommationReglementaire(
			RegDate theTermeGeneralSommationReglementaire) {
		// begin-user-code
		termeGeneralSommationReglementaire = theTermeGeneralSommationReglementaire;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the termeGeneralSommationEffectif
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_otOasC3_Ed2H4bonmeBdag?GETTER"
	 */
	@Column(name = "TERME_GEN_SOMM_EFFECT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getTermeGeneralSommationEffectif() {
		// begin-user-code
		return termeGeneralSommationEffectif;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTermeGeneralSommationEffectif the termeGeneralSommationEffectif to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_otOasC3_Ed2H4bonmeBdag?SETTER"
	 */
	public void setTermeGeneralSommationEffectif(
			RegDate theTermeGeneralSommationEffectif) {
		// begin-user-code
		termeGeneralSommationEffectif = theTermeGeneralSommationEffectif;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the periodefiscale
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BAJV8uCPEd2HTeC2f-Vvpg?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "PERIODE_ID", insertable = false, updatable = false)
	public PeriodeFiscale getPeriodefiscale() {
		// begin-user-code
		return periodefiscale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param thePeriodefiscale the periodefiscale to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BAJV8uCPEd2HTeC2f-Vvpg?SETTER"
	 */
	public void setPeriodefiscale(PeriodeFiscale thePeriodefiscale) {
		// begin-user-code
		periodefiscale = thePeriodefiscale;
		// end-user-code
	}

	@Override
	public ParametrePeriodeFiscale duplicate() {
		return new ParametrePeriodeFiscale(this);
	}
}