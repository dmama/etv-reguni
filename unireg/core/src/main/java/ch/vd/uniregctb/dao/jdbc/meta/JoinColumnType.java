package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Types;

public class JoinColumnType extends ColumnType {
	public JoinColumnType(Class<?> objectClass) {
		super(objectClass, Types.BIGINT);
	}

	public String getConvertMethod(String value) {
		return "get" + getJavaType().getSimpleName() + "(" + value + ")";
	}
}