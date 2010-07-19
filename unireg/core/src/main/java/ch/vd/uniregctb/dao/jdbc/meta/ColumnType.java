package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import ch.vd.registre.base.utils.NotImplementedException;

public abstract class ColumnType {
	protected Class<?> javaType;
	private int sqlType;

	ColumnType(Class<?> javaType, int sqlType) {
		this.javaType = javaType;
		this.sqlType = sqlType;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public Class<?> getSqlType() {
		Class<?> clazz;
		switch (sqlType) {
		case Types.INTEGER:
			clazz = Integer.class;
			break;
		case Types.BIGINT:
			clazz = Long.class;
			break;
		case Types.VARCHAR:
			clazz = String.class;
			break;
		case Types.DATE:
			clazz = Date.class;
			break;
		case Types.TIMESTAMP:
			clazz = Timestamp.class;
			break;
		case Types.BOOLEAN:
			clazz = Boolean.class;
			break;
		default:
			throw new NotImplementedException("Le type SQL = " + sqlType + " n'est pas enregistré");
		}
		return clazz;
	}

	public String getResultGetter() {
		String getter;
		switch (sqlType) {
		case Types.INTEGER:
			getter = "getInt";
			break;
		case Types.BIGINT:
			getter = "getLong";
			break;
		case Types.VARCHAR:
			getter = "getString";
			break;
		case Types.DATE:
			getter = "getDate";
			break;
		case Types.TIMESTAMP:
			getter = "getTimestamp";
			break;
		case Types.BOOLEAN:
			getter = "getBoolean";
			break;
		default:
			throw new NotImplementedException("Le type SQL = " + sqlType + " n'est pas enregistré");
		}
		return getter;
	}
}
