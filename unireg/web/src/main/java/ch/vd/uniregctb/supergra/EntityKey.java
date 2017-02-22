package ch.vd.uniregctb.supergra;

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * La clé d'une entité Hibernate (classe + id).
 */
public class EntityKey {

	private final EntityType type;
	private final Long id;

	public EntityKey(EntityType type, Long id) {
		this.type = type;
		this.id = id;
	}

	public EntityKey(Class<? extends HibernateEntity> clazz, Long id) {
		this.type = findEntityType(clazz);
		this.id = id;
	}

	private static EntityType findEntityType(Class<? extends HibernateEntity> clazz) {
		for (EntityType e : EntityType.values()) {
			if (e.getHibernateClass().isAssignableFrom(clazz)) {
				return e;
			}
		}
		throw new IllegalArgumentException("La classe [" + clazz + "] n'est pas une entité hibernate reconnue.");
	}

	public EntityType getType() {
		return type;
	}

	public Long getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final EntityKey entityKey = (EntityKey) o;

		return id.equals(entityKey.id) && type == entityKey.type;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + id.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return (type == null ? "(unknown)" : type.getDisplayArticleName()) + " n°" + id;
	}

	public String toStringWithPreposition() {
		return (type == null ? "(unknown)" : type.getDisplayPrepositionName()) + " n°" + id;
	}
}

