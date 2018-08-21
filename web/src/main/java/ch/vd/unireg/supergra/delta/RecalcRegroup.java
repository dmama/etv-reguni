package ch.vd.unireg.supergra.delta;

import java.util.Collections;
import java.util.List;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.supergra.EntityKey;
import ch.vd.unireg.supergra.SuperGraContext;

/**
 * Recalcul des regroupements sur une communanuté RF (voir SIFISC-29450).
 * <p/>
 * <b>Note :</b> on ne permet pas à l'utilisateur de modifier lui-même les regroupements avec SuperGra, parce que :
 * <ul>
 * <li>l'algorithme est très compliqué et il très difficile de le reproduire à la main ;</li>
 * <li>si l'utilisateur calcule des valeurs fausses (notamment le 'membresHashCode' du modèle de communauté), on peut casser tout l'algorithme du regroupement des communautés.</li>
 * </ul>
 */
public class RecalcRegroup extends Delta {

	private final EntityKey key;

	public RecalcRegroup(EntityKey key) {
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
		final CommunauteRF communaute = (CommunauteRF) entity;
		context.recalculeRegroupements(communaute);
	}

	@Override
	public String getHtml() {
		return "Recalcul des regroupements " + key.toStringWithPreposition();
	}
}
