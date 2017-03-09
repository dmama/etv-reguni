package ch.vd.uniregctb.hibernate.meta;

public class JoinPropertyType extends PropertyType {
	public JoinPropertyType(Class<?> javaType, Class<?> storageType) {
		super(javaType, storageType);
	}

	public JoinPropertyType(Class<?> storageType) {
		super(storageType, storageType);
	}
}