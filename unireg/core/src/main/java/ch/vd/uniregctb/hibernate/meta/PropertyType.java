package ch.vd.uniregctb.hibernate.meta;

import java.util.HashMap;
import java.util.Map;

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
	public static final BigDecimalPropertyType bigDecimalPropertyType = new BigDecimalPropertyType();

	public static final Map<Class<?>, PropertyType> byJavaType = new HashMap<>();

	static {
		byJavaType.put(longPropType.getJavaType(), longPropType);
		byJavaType.put(Long.TYPE, longPropType);
		byJavaType.put(integerPropType.getJavaType(), integerPropType);
		byJavaType.put(Integer.TYPE, integerPropType);
		byJavaType.put(stringPropType.getJavaType(), stringPropType);
		byJavaType.put(datePropType.getJavaType(), datePropType);
		byJavaType.put(booleanPropType.getJavaType(), booleanPropType);
		byJavaType.put(Boolean.TYPE, booleanPropType);
		byJavaType.put(timestampPropType.getJavaType(), timestampPropType);
		byJavaType.put(bigDecimalPropertyType.getJavaType(), bigDecimalPropertyType);
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
}
