package ch.vd.uniregctb.hibernate.meta;

/**
 * Classe qui expose de manière pratique les méta-informations d'une propriété d'une entité Hibernate.
 */
public class Property implements Comparable<Property> {

	private final String name;
	private final PropertyType type;
	private final String columnName;
	private final String discriminatorValue;
	private final boolean primaryKey;
	private final boolean parentForeignKey;
	private final boolean collection;
	private int index;

	public Property(String name, PropertyType type, String columnName, String discriminatorValue, boolean primaryKey, boolean parentForeignKey, boolean collection) {
		this.name = name;
		this.type = type;
		this.columnName = columnName;
		this.discriminatorValue = discriminatorValue;
		this.primaryKey = primaryKey;
		this.parentForeignKey = parentForeignKey;
		this.collection = collection;
		this.index = 0;
	}

	/**
	 * @return le nom de la propriété (par exemple <i>tiers</i> pour la méthode <i>getTiers</i>)
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return le type de la propriété
	 */
	public PropertyType getType() {
		return type;
	}

	/**
	 * @return le nom de la colonne utilisée dans la base de données
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * @return <i>vrai</i> si la propriété est le discriminant d'une entité Hibernate faisant partie d'une hiérarchie de classes stockées à plat dans une seule table.
	 */
	public boolean isDiscriminator() {
		return discriminatorValue != null;
	}

	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	/**
	 * @return <i>vrai</i> si la propriété est la clé primaire de l'entité Hibernate associée.
	 */
	public boolean isPrimaryKey() {
		return primaryKey;
	}

	/**
	 * @return <i>vrai</i> si la propriété est la clé étrangère de l'entité Hibernate parente (= celle qui possède l'entité Hibernate courante)
	 */
	public boolean isParentForeignKey() {
		return parentForeignKey;
	}

	/**
	 * @return <i>vrai</i> si la propriété est une collection.
	 */
	public boolean isCollection() {
		return collection;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int compareTo(Property o) {
		if (discriminatorValue != null) {
			return -1;
		}
		else if (o.discriminatorValue != null) {
			return 1;
		}
		else if (primaryKey) {
			return -1;
		}
		else if (o.primaryKey) {
			return 1;
		}
		return columnName.compareTo(o.columnName);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final Property property = (Property) o;

		return columnName.equals(property.columnName);
	}

	@Override
	public int hashCode() {
		return columnName.hashCode();
	}
}
