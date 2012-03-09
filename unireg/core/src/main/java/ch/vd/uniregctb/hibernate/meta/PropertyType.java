package ch.vd.uniregctb.hibernate.meta;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Classe qui expose de manière pratique les méta-informations d'un type de propriété d'une entité Hibernate.
 */
public abstract class PropertyType {

	public static final LongPropertyType longPropType = new LongPropertyType();
	public static final IntegerPropertyType integerPropType = new IntegerPropertyType();
	public static final StringPropertyType stringPropType = new StringPropertyType();
	public static final DatePropertyType datePropType = new DatePropertyType();
	public static final BooleanPropertyType booleanPropType = new BooleanPropertyType();
	public static final TimestampPropertyType timestampPropType = new TimestampPropertyType();

	public static final Map<Class<?>, PropertyType> byJavaType = new HashMap<Class<?>, PropertyType>();

	static {
		byJavaType.put(longPropType.getJavaType(), longPropType);
		byJavaType.put(integerPropType.getJavaType(), integerPropType);
		byJavaType.put(Integer.TYPE, integerPropType);
		byJavaType.put(stringPropType.getJavaType(), stringPropType);
		byJavaType.put(datePropType.getJavaType(), datePropType);
		byJavaType.put(booleanPropType.getJavaType(), booleanPropType);
		byJavaType.put(Boolean.TYPE, booleanPropType);
		byJavaType.put(timestampPropType.getJavaType(), timestampPropType);
	}

	protected final Class<?> javaType;
	private final int sqlType;

	PropertyType(Class<?> javaType, int sqlType) {
		this.javaType = javaType;
		this.sqlType = sqlType;
	}

	/**
	 * @return le type java de la propriété
	 */
	public Class<?> getJavaType() {
		return javaType;
	}

	/**
	 * @return le type simple utilisé pour stocker les valeurs dans la base de données
	 */
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

	/**
	 * @return le nom du getter utilisé pour extraire la valeur du result set.
	 */
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

	/**
	 * @return <i>vrai</i> s'il est nécessaire de vérifier la nullité après l'extraction de la valeur du results set
	 */
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
