package ch.vd.unireg.hibernate.meta;

import java.sql.Timestamp;

public class TimestampPropertyType extends PropertyType {
	public TimestampPropertyType() {
		super(Timestamp.class, Timestamp.class);
	}
}
