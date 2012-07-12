package ch.vd.uniregctb.common;

/**
 * Représente la clé permettant d'identifier de manière unique une entité hibernate.
 */
public class EntityKey {
	
	private Class<?> clazz;
	private Object id;

	public EntityKey() {
	}

	public EntityKey(Class<?> clazz, Object id) {
		this.clazz = clazz;
		this.id = id;
	}

	public EntityKey(HibernateEntity entity) {
		clazz = entity.getClass();
		id = entity.getKey();
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final EntityKey entityKey = (EntityKey) o;

		return clazz.equals(entityKey.clazz) && id.equals(entityKey.id);

	}

	@Override
	public int hashCode() {
		int result = clazz.hashCode();
		result = 31 * result + id.hashCode();
		return result;
	}
}
