package ch.vd.uniregctb.activation.manager;

import ch.vd.uniregctb.activation.ActivationServiceException;
import ch.vd.uniregctb.activation.view.TiersReactivationRecapView;

public interface TiersReactivationRecapManager {

	/**
	 * Alimente la vue TiersReactivationRecapView
	 *
	 * @param numeroTiers
	 * @return
	 */
	TiersReactivationRecapView get(Long numeroTiers);

	/**
	 * Persiste le tiers
	 *
	 * @param tiersReactivationRecapView
	 */
	void save(TiersReactivationRecapView tiersReactivationRecapView) throws ActivationServiceException;

}
