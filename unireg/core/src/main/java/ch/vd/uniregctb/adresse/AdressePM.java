/**
 *
 */
package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeAdressePM;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_scmgkOxCEdyck8Nd0o6HOA"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_scmgkOxCEdyck8Nd0o6HOA"
 */
@Entity
@DiscriminatorValue("AdressePM")
public class AdressePM extends AdresseTiers {

	private static final long serialVersionUID = -3569739649334466017L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_scmgk-xCEdyck8Nd0o6HOA"
	 */
	private TypeAdressePM type;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the type
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_scmgk-xCEdyck8Nd0o6HOA?GETTER"
	 */
	@Column(name = "TYPE", length = LengthConstants.ADRESSE_TYPEPM)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAdressePMUserType")
	public TypeAdressePM getType() {
		// begin-user-code
		return type;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theType the type to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_scmgk-xCEdyck8Nd0o6HOA?SETTER"
	 */
	public void setType(TypeAdressePM theType) {
		// begin-user-code
		type = theType;
		// end-user-code
	}

	public ValidationResults validate() {
		ValidationResults results = super.validate();

		if (isAnnule()) {
			return results;
		}

		if (type == null) {
			results.addError("Le type d'adresse doit être renseigné sur une adresse PM [" + this + "]");
		}
		return results;
	}
}