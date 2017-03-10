package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.IdentifiantDroitRFUserType;

public class IdentifiantDroitRFPropertyType extends UserTypePropertyType {
	public IdentifiantDroitRFPropertyType(IdentifiantDroitRFUserType userType) {
		super(userType, String.class);
	}
}
