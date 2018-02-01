package ch.vd.unireg.hibernate.meta;

import java.util.Date;

public class DatePropertyType extends PropertyType {
	public DatePropertyType() {
		super(Date.class, Date.class);
	}
}
