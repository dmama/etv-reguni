package ch.vd.unireg.hibernate.meta;

import ch.vd.unireg.hibernate.RegDateUserType;

public class RegDatePropertyType extends UserTypePropertyType {
	public RegDatePropertyType(RegDateUserType userType) {
		super(userType, Integer.class);
	}
}
