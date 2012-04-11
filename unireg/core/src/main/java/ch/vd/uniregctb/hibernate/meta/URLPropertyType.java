package ch.vd.uniregctb.hibernate.meta;

import java.net.URL;

import ch.vd.uniregctb.hibernate.URLUserType;

public class URLPropertyType extends UserTypePropertyType {
	public URLPropertyType(URLUserType userType) {
		super(URL.class, userType);
	}

	@Override
	public String getConvertMethod(String value) {
		return "new URL(\"" + value + "\")";
	}
}
