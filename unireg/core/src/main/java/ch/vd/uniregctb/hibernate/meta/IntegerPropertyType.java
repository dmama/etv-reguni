package ch.vd.uniregctb.hibernate.meta;

import java.sql.Types;

public class IntegerPropertyType extends PropertyType {
	public IntegerPropertyType() {
		super(Integer.class, Types.INTEGER);
	}
}
