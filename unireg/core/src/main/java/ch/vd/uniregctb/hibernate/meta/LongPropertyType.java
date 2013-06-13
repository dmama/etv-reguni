package ch.vd.uniregctb.hibernate.meta;

import java.sql.Types;

import ch.vd.uniregctb.hibernate.meta.PropertyType;

public class LongPropertyType extends PropertyType {
	public LongPropertyType() {
		super(Long.class, Types.BIGINT);
	}
}
