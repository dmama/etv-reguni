package ch.vd.uniregctb.hibernate.meta;

import java.sql.Types;

public class BooleanPropertyType extends PropertyType {
	public BooleanPropertyType() {
		super(Boolean.class, Types.BOOLEAN);
	}
}