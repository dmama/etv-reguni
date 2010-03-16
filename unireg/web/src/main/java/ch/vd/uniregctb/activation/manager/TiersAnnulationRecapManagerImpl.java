package ch.vd.uniregctb.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.activation.ActivationService;
import ch.vd.uniregctb.activation.view.TiersAnnulationRecapView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class TiersAnnulationRecapManagerImpl implements TiersAnnulationRecapManager {

	private TiersGeneralManager tiersGeneralManager;

	private TiersService tiersService;

	private ActivationService activationService;

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setActivationService(ActivationService activationService) {
		this.activationService = activationService;
	}

	/**
	 * Alimente la vue TiersAnnulationRecapView
	 *
	 * @param numeroTiers
	 * @return
	 */
	public TiersAnnulationRecapView get(Long numeroTiers)  {
		TiersAnnulationRecapView tiersAnnulationRecapView = new TiersAnnulationRecapView();
		Tiers tiers = tiersService.getTiers(numeroTiers);

		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(tiers);
		tiersAnnulationRecapView.setTiers(tiersGeneralView);
		tiersAnnulationRecapView.setNouveauTiers(true);

		return tiersAnnulationRecapView;
	}

	/**
	 * Alimente la vue TiersAnnulationRecapView
	 *
	 * @param numeroTiers
	 * @param numeroTiersRemplacant
	 * @return
	 */
	public TiersAnnulationRecapView get(Long numeroTiers, Long numeroTiersRemplacant)  {
		TiersAnnulationRecapView tiersAnnulationRecapView = new TiersAnnulationRecapView();
		Tiers tiers = tiersService.getTiers(numeroTiers);
		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(tiers);
		tiersAnnulationRecapView.setTiers(tiersGeneralView);
		Tiers tiersRemplacant = tiersService.getTiers(numeroTiersRemplacant);
		TiersGeneralView tiersRemplacantGeneralView = tiersGeneralManager.get(tiersRemplacant);
		tiersAnnulationRecapView.setTiersRemplacant(tiersRemplacantGeneralView);
		tiersAnnulationRecapView.setNouveauTiers(false);
		return tiersAnnulationRecapView;
	}


	/**
	 * Persiste le tiers
	 *
	 * @param tiersAnnulationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(TiersAnnulationRecapView tiersAnnulationRecapView) {
		Tiers tiers = tiersService.getTiers(tiersAnnulationRecapView.getTiers().getNumero());
		if (tiersAnnulationRecapView.getTiersRemplacant() != null) {
			Tiers tiersRemplacant = tiersService.getTiers(tiersAnnulationRecapView.getTiersRemplacant().getNumero());
			activationService.remplaceTiers(tiers, tiersRemplacant, tiersAnnulationRecapView.getDateAnnulation());
		}
		else {
			activationService.annuleTiers(tiers, tiersAnnulationRecapView.getDateAnnulation());
		}

	}

}
