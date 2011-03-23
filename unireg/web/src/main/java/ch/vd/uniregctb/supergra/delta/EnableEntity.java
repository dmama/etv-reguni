package ch.vd.uniregctb.supergra.delta;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.SuperGraContext;

/**
 * Désannulation d'une entité.
 */
public class EnableEntity extends Delta {

	private EntityKey key;

	public EnableEntity(EntityKey key) {
		this.key = key;
	}

	@Override
	public EntityKey getKey() {
		return key;
	}

	@Override
	public void apply(HibernateEntity entity, SuperGraContext context) {
		entity.setAnnule(false);
	}

	@Override
	public String getHtml() {
		return "Désannulation de " + key;
	}
}