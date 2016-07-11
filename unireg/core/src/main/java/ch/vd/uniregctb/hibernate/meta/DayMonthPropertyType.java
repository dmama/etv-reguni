package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.DayMonthUserType;
import ch.vd.uniregctb.type.DayMonth;

public class DayMonthPropertyType extends UserTypePropertyType {

	public DayMonthPropertyType(DayMonthUserType userType) {
		super(DayMonth.class, userType);
	}

	@Override
	public String getConvertMethod(String value) {
		return "DayMonth.fromIndex(" + value + ")";
	}
}
