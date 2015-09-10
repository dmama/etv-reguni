package ch.vd.uniregctb.hibernate.meta;

import java.sql.Types;

import ch.vd.uniregctb.hibernate.meta.PropertyType;

public class StringPropertyType extends PropertyType {
	public StringPropertyType() {
		super(String.class, Types.VARCHAR);
	}
}
