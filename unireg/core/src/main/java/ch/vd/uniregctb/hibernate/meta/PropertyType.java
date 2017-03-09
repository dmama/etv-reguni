package ch.vd.uniregctb.hibernate.meta;

/**
 * Classe qui expose de manière pratique les méta-informations d'un type de propriété d'une entité Hibernate.
 */
public abstract class PropertyType {

	protected final Class<?> javaType;
	protected final Class<?> storageType;

	PropertyType(Class<?> javaType, Class<?> storageType) {
		this.javaType = javaType;
		this.storageType = storageType;
	}

	/**
	 * @return le type concret de la propriété, tel qu'exposé par les setters/getters.
	 */
	public Class<?> getJavaType() {
		return javaType;
	}

	/**
	 * @return le type utilisé pour le stockage dans la base de données (par exemple: Long.class pour l'id d'une entité hibernate)
	 */
	public Class<?> getStorageType() {
		return storageType;
	}
}
