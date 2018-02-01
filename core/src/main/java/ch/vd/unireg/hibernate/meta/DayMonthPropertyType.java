package ch.vd.unireg.hibernate.meta;

import ch.vd.unireg.hibernate.DayMonthUserType;

public class DayMonthPropertyType extends UserTypePropertyType {
	public DayMonthPropertyType(DayMonthUserType userType) {
		super(userType, Integer.class);
	}
}
