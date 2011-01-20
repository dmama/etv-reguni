/**
 *
 */
package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeAdresseCivil;

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
	private TypeAdresseCivil type;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the type
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tQ7pwJNcEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "STYPE", length = LengthConstants.ADRESSE_TYPECIVIL)
	@Type(type="ch.vd.uniregctb.hibernate.TypeAdresseCivilLegacyUserType")
	public TypeAdresseCivil getType() {
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
	public void setType(TypeAdresseCivil theType) {
		// begin-user-code
		type = theType;
		// end-user-code
	}
}
