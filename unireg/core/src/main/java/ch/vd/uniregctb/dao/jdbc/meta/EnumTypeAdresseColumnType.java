package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Types;

import org.hibernate.usertype.UserType;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.uniregctb.hibernate.EnumTypeAdresseUserType;

public class EnumTypeAdresseColumnType extends UserTypeColumnType {

	public EnumTypeAdresseColumnType(EnumTypeAdresseUserType userType) {
		super(EnumTypeAdresse.class, userType);
	}

	public String getConvertMethod(String value) {
		return "EnumTypeAdresse.getEnum(" + value + ")";
	}
}