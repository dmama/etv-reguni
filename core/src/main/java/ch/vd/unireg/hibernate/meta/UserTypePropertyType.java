package ch.vd.unireg.hibernate.meta;

import org.hibernate.usertype.UserType;

public abstract class UserTypePropertyType extends PropertyType {
	public UserTypePropertyType(UserType userType, Class<?> storageType) {
		super(userType.returnedClass(), storageType);
	}
}
