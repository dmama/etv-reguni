package ch.vd.unireg.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.activation.view.TiersAnnulationRecapView;

public interface TiersAnnulationRecapManager {

	/**
	 * Alimente la vue TiersAnnulationRecapView
	 *
	 * @param numeroTiers
	 * @return
	 */
	@Transactional(readOnly = true)
	TiersAnnulationRecapView get(Long numeroTiers);

	/**
	 * Alimente la vue TiersAnnulationRecapView
	 *
	 * @param numeroTiers
	 * @param numeroTiersRemplacant
	 * @return
	 */
	@Transactional(readOnly = true)
	TiersAnnulationRecapView get(Long numeroTiers, Long numeroTiersRemplacant)  ;

	/**
	 * Persiste le tiers
	 *
	 * @param tiersAnnulationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(TiersAnnulationRecapView tiersAnnulationRecapView) ;
}
