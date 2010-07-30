/**
 *
 */
package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_qN5loOnXEdyry7KVEl1EMw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_qN5loOnXEdyry7KVEl1EMw"
 */
@Entity
@DiscriminatorValue("AdresseAutreTiers")
public class AdresseAutreTiers extends AdresseTiers {

	private static final long serialVersionUID = 8003143867392448951L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_fwkOoOnYEdyry7KVEl1EMw"
	 */
	private TypeAdresseTiers type;


	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sB3BIenYEdyry7KVEl1EMw"
	 */
	private Long autreTiersId;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the type
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_fwkOoOnYEdyry7KVEl1EMw?GETTER"
	 */
	@Column(name = "TYPE", length = LengthConstants.ADRESSE_TYPETIERS)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAdresseTiersUserType")
	public TypeAdresseTiers getType() {
		// begin-user-code
		return type;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theType the type to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_fwkOoOnYEdyry7KVEl1EMw?SETTER"
	 */
	public void setType(TypeAdresseTiers theType) {
		// begin-user-code
		type = theType;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the autreTiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sB3BIenYEdyry7KVEl1EMw?GETTER"
	 */
	@Column(name = "AUTRE_TIERS_ID")
	@ForeignKey(name = "FK_ADR_AT_TRS_ID")
	@Index(name = "IDX_ADR_AT_TRS_ID", columnNames = "AUTRE_TIERS_ID")
	public Long getAutreTiersId() {
		// begin-user-code
		return autreTiersId;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param id the autreTiers to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sB3BIenYEdyry7KVEl1EMw?SETTER"
	 */
	public void setAutreTiersId(Long id) {
		// begin-user-code
		autreTiersId = id;
		// end-user-code
	}

	public ValidationResults validate() {
		ValidationResults results = new ValidationResults();
		if (type == null) {
			results.addError("Le type d'adresse doit être renseigné sur une adresse autre tiers [" + this + "]");
		}
		return results;
	}
}
