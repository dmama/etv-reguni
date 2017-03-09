package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.EnumUserType;

public class EnumUserTypePropertyType extends UserTypePropertyType {
	public EnumUserTypePropertyType(EnumUserType enumUserType) {
		super(enumUserType, String.class);
	}
}
