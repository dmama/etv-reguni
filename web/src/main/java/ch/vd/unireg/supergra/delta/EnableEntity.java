package ch.vd.unireg.supergra.delta;

import java.util.Collections;
import java.util.List;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.supergra.EntityKey;
import ch.vd.unireg.supergra.SuperGraContext;

/**
 * Désannulation d'une entité.
 */
public class EnableEntity extends Delta {

	private final EntityKey key;

	public EnableEntity(EntityKey key) {
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
		entity.setAnnule(false);
	}

	@Override
	public String getHtml() {
		return "Désannulation " + key.toStringWithPreposition();
	}
}