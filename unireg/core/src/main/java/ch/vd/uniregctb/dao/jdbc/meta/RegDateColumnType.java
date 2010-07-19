package ch.vd.uniregctb.dao.jdbc.meta;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.hibernate.RegDateUserType;

public class RegDateColumnType extends UserTypeColumnType {
	private final RegDateUserType regDateUserType;

	public RegDateColumnType(RegDateUserType regDateUserType) {
		super(RegDate.class, regDateUserType);
		this.regDateUserType = regDateUserType;
	}

	public String getConvertMethod(String value) {
		return "RegDate.fromIndex(" + value + ", " + (regDateUserType.isAllowPartial() ? "true" : "false") + ")";
	}
}
