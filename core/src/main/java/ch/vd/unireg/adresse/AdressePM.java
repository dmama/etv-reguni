/**
 *
 */
package ch.vd.unireg.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TypeAdressePM;

@Entity
@DiscriminatorValue("AdressePM")
public class AdressePM extends AdresseTiers {

	private TypeAdressePM type;

	public AdressePM() {
	}

	protected AdressePM(AdressePM src) {
		super(src);
		this.type = src.type;
	}

	@Column(name = "TYPE_PM", length = LengthConstants.ADRESSE_TYPEPM)
	@Type(type = "ch.vd.unireg.hibernate.TypeAdressePMUserType")
	public TypeAdressePM getType() {
		return type;
	}

	public void setType(TypeAdressePM theType) {
		type = theType;
	}

	@Override
	public AdressePM duplicate() {
		return new AdressePM(this);
	}
}
