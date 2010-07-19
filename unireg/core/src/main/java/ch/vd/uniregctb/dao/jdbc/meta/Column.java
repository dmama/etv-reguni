package ch.vd.uniregctb.dao.jdbc.meta;

public class Column implements Comparable<Column> {
	private boolean discriminator;
	private boolean primaryKey;
	private boolean parentForeignKey;
	private String name;
	private ColumnType type;
	private String property;

	public Column(String name, ColumnType type, String property) {
		this.name = name;
		this.type = type;
		this.property = property;
		this.discriminator = false;
		this.primaryKey = false;
	}

	public Column(String name, ColumnType type, String property, boolean discriminator, boolean primaryKey, boolean parentForeignKey) {
		this.name = name;
		this.type = type;
		this.property = property;
		this.discriminator = discriminator;
		this.primaryKey = primaryKey;
		this.parentForeignKey = parentForeignKey;
	}

	public String getName() {
		return name;
	}

	public ColumnType getType() {
		return type;
	}

	public boolean isDiscriminator() {
		return discriminator;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public boolean isParentForeignKey() {
		return parentForeignKey;
	}

	public int compareTo(Column o) {
		if (discriminator) {
			return -1;
		}
		else if (o.discriminator) {
			return 1;
		}
		else if (primaryKey) {
			return -1;
		}
		else if (o.primaryKey) {
			return 1;
		}
		return name.compareTo(o.name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final Column column = (Column) o;

		return name.equals(column.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public String getProperty() {
		return property;
	}
}
