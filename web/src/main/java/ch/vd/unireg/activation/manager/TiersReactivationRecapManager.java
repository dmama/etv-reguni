package ch.vd.unireg.activation.manager;

import ch.vd.unireg.activation.ActivationServiceException;
import ch.vd.unireg.activation.view.TiersReactivationRecapView;

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
