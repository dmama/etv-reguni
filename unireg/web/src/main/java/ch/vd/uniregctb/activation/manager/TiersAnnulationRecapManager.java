package ch.vd.uniregctb.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.activation.view.TiersAnnulationRecapView;

public interface TiersAnnulationRecapManager {

	/**
	 * Alimente la vue TiersAnnulationRecapView
	 *
	 * @param numeroTiers
	 * @return
	 */
	public TiersAnnulationRecapView get(Long numeroTiers);

	/**
	 * Alimente la vue TiersAnnulationRecapView
	 *
	 * @param numeroTiers
	 * @param numeroTiersRemplacant
	 * @return
	 */
	public TiersAnnulationRecapView get(Long numeroTiers, Long numeroTiersRemplacant)  ;

	/**
	 * Persiste le tiers
	 *
	 * @param tiersAnnulationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(TiersAnnulationRecapView tiersAnnulationRecapView) ;
}
