package ch.vd.uniregctb.hibernate.meta;

public class CollectionPropertyType extends PropertyType {

	public CollectionPropertyType(Class<?> javaType) {
		super(javaType, -1);
	}

	@Override
	public Class<?> getSqlType() {
		throw new UnsupportedOperationException("Les collections ne sont pas représentées par un type SQL");
	}
}
