package ch.vd.uniregctb.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.activation.view.TiersReactivationRecapView;

public interface TiersReactivationRecapManager {

	/**
	 * Alimente la vue TiersReactivationRecapView
	 *
	 * @param numeroTiers
	 * @return
	 */
	public TiersReactivationRecapView get(Long numeroTiers);

	/**
	 * Persiste le tiers
	 *
	 * @param tiersReactivationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(TiersReactivationRecapView tiersReactivationRecapView);

}
