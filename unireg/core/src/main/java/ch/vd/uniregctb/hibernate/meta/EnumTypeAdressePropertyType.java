package ch.vd.uniregctb.hibernate.meta;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.uniregctb.hibernate.EnumTypeAdresseUserType;

public class EnumTypeAdressePropertyType extends UserTypePropertyType {

	public EnumTypeAdressePropertyType(EnumTypeAdresseUserType userType) {
		super(EnumTypeAdresse.class, userType);
	}

	public String getConvertMethod(String value) {
		return "EnumTypeAdresse.getEnum(" + value + ")";
	}
}