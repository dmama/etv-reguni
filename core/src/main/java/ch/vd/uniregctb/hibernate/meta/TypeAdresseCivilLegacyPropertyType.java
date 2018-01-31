package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.TypeAdresseCivilLegacyUserType;

public class TypeAdresseCivilLegacyPropertyType extends UserTypePropertyType {
	public TypeAdresseCivilLegacyPropertyType(TypeAdresseCivilLegacyUserType userType) {
		super(userType, String.class);
	}
}