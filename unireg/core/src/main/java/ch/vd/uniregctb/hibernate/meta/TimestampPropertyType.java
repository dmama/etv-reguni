package ch.vd.uniregctb.hibernate.meta;

import java.sql.Timestamp;
import java.sql.Types;

import ch.vd.uniregctb.hibernate.meta.PropertyType;

public class TimestampPropertyType extends PropertyType {
	public TimestampPropertyType() {
		super(Timestamp.class, Types.TIMESTAMP);
	}
}
