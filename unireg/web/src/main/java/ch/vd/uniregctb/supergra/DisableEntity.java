package ch.vd.uniregctb.supergra;

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * Annulation d'une entité.
 */
public class DisableEntity extends Delta {

	private EntityKey key;

	public DisableEntity(EntityKey key) {
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
