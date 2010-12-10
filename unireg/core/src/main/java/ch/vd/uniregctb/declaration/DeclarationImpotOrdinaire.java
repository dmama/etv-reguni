package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Date;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7a2SYOqeEdySTq6PFlf9jQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7a2SYOqeEdySTq6PFlf9jQ"
 */
@Entity
@DiscriminatorValue("DI")
public class DeclarationImpotOrdinaire extends Declaration {

	private static final long serialVersionUID = -4869699873165367700L;

	/**
	 * <!-- begin-user-doc -->
	 * Numéro de séquence de la déclaration pour une période fiscale. La première déclaration prends le numéro 1.
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_wnJNUOqgEdySTq6PFlf9jQ"
	 */
	private Integer numero;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_po8_IAI5Ed2twI8L5z7uGQ"
	 */
	private Integer numeroOfsForGestion;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sGA-oDfHEd2EkOqealhanQ"
	 */
	private TypeContribuable typeContribuable;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ov4TEDJSEd2Q1vtOul__sQ"
	 */
	private Date dateImpressionChemiseTaxationOffice;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ov4TEDJSEd2Q1vtOul__sQ"
	 */
	private Qualification qualification;

	private RegDate delaiRetourImprime;

	private Long retourCollectiviteAdministrativeId;

	/**
	 * <code>true</code> si la DI a été créée comme une "di libre", c'est-à-dire une DI sur la période courante (au moment de sa création)
	 * sans fin d'assujettissement connue (comme un décès ou un départ HS)
	 */
	private boolean libre;

	@Column(name = "RETOUR_COLL_ADMIN_ID")
	@ForeignKey(name = "FK_DECL_RET_COLL_ADMIN_ID")
	public Long getRetourCollectiviteAdministrativeId() {
		return retourCollectiviteAdministrativeId;
	}

	public void setRetourCollectiviteAdministrativeId(Long retourCollectiviteAdministrativeId) {
		this.retourCollectiviteAdministrativeId = retourCollectiviteAdministrativeId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numero
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_wnJNUOqgEdySTq6PFlf9jQ?GETTER"
	 */
	@Column(name = "NUMERO")
	public Integer getNumero() {
		// begin-user-code
		return numero;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumero the numero to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_wnJNUOqgEdySTq6PFlf9jQ?SETTER"
	 */
	public void setNumero(Integer theNumero) {
		// begin-user-code
		numero = theNumero;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the typeContribuable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sGA-oDfHEd2EkOqealhanQ?GETTER"
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
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sGA-oDfHEd2EkOqealhanQ?SETTER"
	 */
	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		// begin-user-code
		typeContribuable = theTypeContribuable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateImpressionChemiseTaxationOffice
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ov4TEDJSEd2Q1vtOul__sQ?GETTER"
	 */
	@Column(name = "DATE_IMPR_CHEMISE_TO")
	public Date getDateImpressionChemiseTaxationOffice() {
		// begin-user-code
		return dateImpressionChemiseTaxationOffice;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateImpressionChemiseTaxationOffice the dateImpressionChemiseTaxationOffice to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ov4TEDJSEd2Q1vtOul__sQ?SETTER"
	 */
	public void setDateImpressionChemiseTaxationOffice(Date theDateImpressionChemiseTaxationOffice) {
		// begin-user-code
		dateImpressionChemiseTaxationOffice = theDateImpressionChemiseTaxationOffice;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the qualification
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sGA-oDfHEd2EkOqealhanQ?GETTER"
	 */
	@Column(name = "QUALIFICATION", length = LengthConstants.DI_QUALIF )
	@Type(type = "ch.vd.uniregctb.hibernate.QualificationUserType")
	public Qualification getQualification() {
		return qualification;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param Qualification the qualification to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sGA-oDfHEd2EkOqealhanQ?SETTER"
	 */
	public void setQualification(Qualification qualification) {
		this.qualification = qualification;
	}

	/**
	 * [UNIREG-1740] Le délai de retour tel que devant être imprimé sur le déclaration papier. Ce délai peut être nul, auquel cas on utilisera le délai accordé comme valeur de remplacement.
	 *
	 * @return une date correspondant au délai de retour; ou <i>null</i> si l'information n'est pas disponible.
	 */
	@Column(name = "DELAI_RETOUR_IMPRIME")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDelaiRetourImprime() {
		return delaiRetourImprime;
	}

	public void setDelaiRetourImprime(RegDate delaiRetourImprime) {
		this.delaiRetourImprime = delaiRetourImprime;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroOfsForGestion
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_po8_IAI5Ed2twI8L5z7uGQ?GETTER"
	 */
	@Column(name = "NO_OFS_FOR_GESTION")
	public Integer getNumeroOfsForGestion() {
		// begin-user-code
		return numeroOfsForGestion;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroOfsForGestion the numeroOfsForGestion to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_po8_IAI5Ed2twI8L5z7uGQ?SETTER"
	 */
	public void setNumeroOfsForGestion(Integer theNumeroOfsForGestion) {
		// begin-user-code
		numeroOfsForGestion = theNumeroOfsForGestion;
		// end-user-code
	}

	@Column(name = "LIBRE")
	public boolean isLibre() {
		return libre;
	}

	public void setLibre(boolean libre) {
		this.libre = libre;
	}

	@Transient
	public TypeDocument getTypeDeclaration() {
		final ModeleDocument modele = getModeleDocument();
		if (modele == null) {
			return null;
		}
		return modele.getTypeDocument();
	}
}
