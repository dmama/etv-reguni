package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.RegDateUserType;

public class RegDatePropertyType extends UserTypePropertyType {
	public RegDatePropertyType(RegDateUserType userType) {
		super(userType, Integer.class);
	}
}
