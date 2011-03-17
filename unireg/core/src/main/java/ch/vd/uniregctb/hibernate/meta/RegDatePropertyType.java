package ch.vd.uniregctb.hibernate.meta;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.hibernate.RegDateUserType;

public class RegDatePropertyType extends UserTypePropertyType {
	private final RegDateUserType regDateUserType;

	public RegDatePropertyType(RegDateUserType regDateUserType) {
		super(RegDate.class, regDateUserType);
		this.regDateUserType = regDateUserType;
	}

	public String getConvertMethod(String value) {
		return "RegDate.fromIndex(" + value + ", " + (regDateUserType.isAllowPartial() ? "true" : "false") + ")";
	}
}
