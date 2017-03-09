package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.DayMonthUserType;

public class DayMonthPropertyType extends UserTypePropertyType {
	public DayMonthPropertyType(DayMonthUserType userType) {
		super(userType, Integer.class);
	}
}
