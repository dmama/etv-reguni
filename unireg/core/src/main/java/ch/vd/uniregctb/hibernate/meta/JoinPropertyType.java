package ch.vd.uniregctb.hibernate.meta;

import java.sql.Types;

public class JoinPropertyType extends PropertyType {
	public JoinPropertyType(Class<?> objectClass) {
		super(objectClass, Types.BIGINT);
	}
}