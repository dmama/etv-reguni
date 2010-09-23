/**
 *
 */
package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * Adresse courrier de l'individu ou de l'un des individus du ménage commun
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi921x9Edygsbnw9h5bVw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi921x9Edygsbnw9h5bVw"
 */
@Entity
@DiscriminatorValue("AdresseCivile")
public class AdresseCivile extends AdresseTiers {

	private static final long serialVersionUID = -4200736623457155968L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tQ7pwJNcEdygKK6Oe0tVlw"
	 */
	private EnumTypeAdresse type;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the type
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tQ7pwJNcEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "STYPE", length = LengthConstants.ADRESSE_TYPECIVIL)
	@Type(type="ch.vd.uniregctb.hibernate.EnumTypeAdresseUserType")
	public EnumTypeAdresse getType() {
		// begin-user-code
		return type;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theType the type to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tQ7pwJNcEdygKK6Oe0tVlw?SETTER"
	 */
	public void setType(EnumTypeAdresse theType) {
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
			results.addError("Le type d'adresse doit être renseigné sur une adresse civile [" + this + "]");
		}
		return results;
	}
}