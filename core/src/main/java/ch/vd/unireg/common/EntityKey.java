package ch.vd.unireg.common;

import java.util.Objects;

/**
 * Représente la clé permettant d'identifier de manière unique une entité hibernate.
 */
public class EntityKey {
	
	private final Class<? extends HibernateEntity> clazz;
	private final Object id;

	public EntityKey(Class<? extends HibernateEntity> clazz, Object id) {
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(id);
		this.clazz = clazz;
		this.id = id;
	}

	public EntityKey(HibernateEntity entity) {
		Objects.requireNonNull(entity);
		Objects.requireNonNull(entity.getKey());
		clazz = entity.getClass();
		id = entity.getKey();
	}

	public Class<? extends HibernateEntity> getClazz() {
		return clazz;
	}

	public Object getId() {
		return id;
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
