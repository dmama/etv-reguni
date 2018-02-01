package ch.vd.unireg.hibernate.meta;

import ch.vd.unireg.hibernate.TypeAdresseCivilLegacyUserType;

public class TypeAdresseCivilLegacyPropertyType extends UserTypePropertyType {
	public TypeAdresseCivilLegacyPropertyType(TypeAdresseCivilLegacyUserType userType) {
		super(userType, String.class);
	}
}