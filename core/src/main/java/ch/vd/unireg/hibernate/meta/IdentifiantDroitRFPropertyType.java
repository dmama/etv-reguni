package ch.vd.unireg.hibernate.meta;

import ch.vd.unireg.hibernate.IdentifiantDroitRFUserType;

public class IdentifiantDroitRFPropertyType extends UserTypePropertyType {
	public IdentifiantDroitRFPropertyType(IdentifiantDroitRFUserType userType) {
		super(userType, String.class);
	}
}
