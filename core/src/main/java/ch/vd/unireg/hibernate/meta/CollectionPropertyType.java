package ch.vd.unireg.hibernate.meta;

public class CollectionPropertyType extends PropertyType {
	public CollectionPropertyType(Class<?> javaType) {
		super(javaType, javaType);
	}
}
