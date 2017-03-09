package ch.vd.uniregctb.hibernate.meta;

public class CollectionPropertyType extends PropertyType {
	public CollectionPropertyType(Class<?> javaType) {
		super(javaType, javaType);
	}
}
