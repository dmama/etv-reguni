/**
 *
 */
package ch.vd.unireg.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TypeAdresseCivil;

/**
 * Adresse courrier de l'individu ou de l'un des individus du ménage commun
 */
@Entity
@DiscriminatorValue("AdresseCivile")
public class AdresseCivile extends AdresseTiers {

	private TypeAdresseCivil type;

	public AdresseCivile() {
	}

	protected AdresseCivile(AdresseCivile src) {
		super(src);
		this.type = src.type;
	}

	@Column(name = "STYPE", length = LengthConstants.ADRESSE_TYPECIVIL)
	@Type(type="ch.vd.unireg.hibernate.TypeAdresseCivilLegacyUserType")
	public TypeAdresseCivil getType() {
		return type;
	}

	public void setType(TypeAdresseCivil theType) {
		type = theType;
	}

	@Override
	public AdresseCivile duplicate() {
		return new AdresseCivile(this);
	}
}
