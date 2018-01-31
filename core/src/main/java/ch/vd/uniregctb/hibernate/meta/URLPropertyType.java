package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.URLUserType;

public class URLPropertyType extends UserTypePropertyType {
	public URLPropertyType(URLUserType userType) {
		super(userType, String.class);
	}
}
