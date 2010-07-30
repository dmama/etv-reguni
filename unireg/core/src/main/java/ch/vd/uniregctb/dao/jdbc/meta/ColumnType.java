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
			clazz = Integer.TYPE;
			break;
		case Types.BIGINT:
			clazz = Long.TYPE;
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
			clazz = Boolean.TYPE;
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

	public boolean needNullCheck() {
		boolean check;
		switch (sqlType) {
		case Types.INTEGER:
			check = true;
			break;
		case Types.BIGINT:
			check = true;
			break;
		case Types.VARCHAR:
			check = false;
			break;
		case Types.DATE:
			check = false;
			break;
		case Types.TIMESTAMP:
			check = false;
			break;
		case Types.BOOLEAN:
			check = true;
			break;
		default:
			throw new NotImplementedException("Le type SQL = " + sqlType + " n'est pas enregistré");
		}
		return check;
	}
}
