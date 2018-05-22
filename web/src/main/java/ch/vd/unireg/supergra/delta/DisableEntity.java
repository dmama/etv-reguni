package ch.vd.unireg.supergra.delta;

import java.util.Collections;
import java.util.List;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.supergra.EntityKey;
import ch.vd.unireg.supergra.SuperGraContext;

/**
 * Annulation d'une entité.
 */
public class DisableEntity extends Delta {

	private final EntityKey key;

	public DisableEntity(EntityKey key) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		this.key = key;
	}

	@Override
	public EntityKey getKey() {
		return key;
	}

	@Override
	public List<EntityKey> getAllKeys() {
		return Collections.singletonList(key);
	}

	@Override
	public void apply(HibernateEntity entity, SuperGraContext context) {
		entity.setAnnule(true);
	}

	@Override
	public String getHtml() {
		return "Annulation " + key.toStringWithPreposition();
	}
}
