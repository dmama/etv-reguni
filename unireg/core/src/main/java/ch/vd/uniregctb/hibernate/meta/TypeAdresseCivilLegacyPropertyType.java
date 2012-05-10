package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.TypeAdresseCivilLegacyUserType;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class TypeAdresseCivilLegacyPropertyType extends UserTypePropertyType {

	public TypeAdresseCivilLegacyPropertyType(TypeAdresseCivilLegacyUserType userType) {
		super(TypeAdresseCivil.class, userType);
	}

	@Override
	public String getConvertMethod(String value) {
		return "TypeAdresseCivil.fromDbValue(" + value + ")";
	}
}