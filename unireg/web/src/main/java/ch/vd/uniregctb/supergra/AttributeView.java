package ch.vd.uniregctb.supergra;

/**
 * Vue d'un attribut d'une entité Hibernate édité dans le mode SuperGra.
 */
public class AttributeView {

	private String name;
	private Class<?> type;
	private Object value;
	private boolean parentForeignKey;
	private EntityType parentEntityType;
	private boolean collection;
	private boolean readonly;

	public AttributeView(String name, Class<?> type, Object value, boolean isParentForeignKey, boolean isCollection, boolean isReadonly) {
		this.name = name;
		this.type = type;
		this.value = value;
		this.parentForeignKey = isParentForeignKey;
		if (isParentForeignKey && value != null) {
			this.parentEntityType = EntityType.fromHibernateClass(value.getClass());
		}
		else {
			this.parentEntityType = null;
		}
		this.collection = isCollection;
		this.readonly = isReadonly;
	}

	public AttributeView(AttributeView right) {
		this.name = right.name;
		this.type = right.type;
		this.value = right.value;
		this.parentForeignKey = right.parentForeignKey;
		this.parentEntityType = right.parentEntityType;
		this.collection = right.collection;
		this.readonly = right.readonly;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public boolean isParentForeignKey() {
		return parentForeignKey;
	}

	public EntityType getParentEntityType() {
		return parentEntityType;
	}

	public boolean isCollection() {
		return collection;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
}
