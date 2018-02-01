package ch.vd.unireg.hibernate.meta;

import ch.vd.unireg.hibernate.URLUserType;

public class URLPropertyType extends UserTypePropertyType {
	public URLPropertyType(URLUserType userType) {
		super(userType, String.class);
	}
}
