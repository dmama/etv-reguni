package ch.vd.uniregctb.supergra;

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
		return (type == null ? "(unknown)" : type.getDisplayName()) + " n°" + id;
	}
}

