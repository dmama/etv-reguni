package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.utils.NotImplementedException;

public abstract class ColumnType {

	public static final LongColumnType longColumnType = new LongColumnType();
	public static final IntegerColumnType integerColumnType = new IntegerColumnType();
	public static final StringColumnType stringColumnType = new StringColumnType();
	public static final DateColumnType dateColumnType = new DateColumnType();
	public static final BooleanColumnType booleanColumnType = new BooleanColumnType();
	public static final TimestampColumnType timestampColumnType = new TimestampColumnType();

	public static final Map<Class<?>, ColumnType> byJavaType = new HashMap<Class<?>, ColumnType>();

	static {
		byJavaType.put(longColumnType.getJavaType(), longColumnType);
		byJavaType.put(integerColumnType.getJavaType(), integerColumnType);
		byJavaType.put(Integer.TYPE, integerColumnType);
		byJavaType.put(stringColumnType.getJavaType(), stringColumnType);
		byJavaType.put(dateColumnType.getJavaType(), dateColumnType);
		byJavaType.put(booleanColumnType.getJavaType(), booleanColumnType);
		byJavaType.put(Boolean.TYPE, booleanColumnType);
		byJavaType.put(timestampColumnType.getJavaType(), timestampColumnType);
	}

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
