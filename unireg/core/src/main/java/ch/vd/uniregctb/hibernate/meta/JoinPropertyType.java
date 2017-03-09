package ch.vd.uniregctb.hibernate.meta;

public class JoinPropertyType extends PropertyType {
	public JoinPropertyType(Class<?> storageType) {
		super(storageType, storageType);
	}
}