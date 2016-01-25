package ch.vd.uniregctb.supergra.delta;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.SuperGraContext;

/**
 * Annulation d'une entité.
 */
public class DisableEntity extends Delta {

	private final EntityKey key;

	public DisableEntity(EntityKey key) {
		Assert.notNull(key);
		this.key = key;
	}

	@Override
	public EntityKey getKey() {
		return key;
	}

	@Override
	public void apply(HibernateEntity entity, SuperGraContext context) {
		entity.setAnnule(true);
	}

	@Override
	public String getHtml() {
		return "Annulation de " + key;
	}
}
