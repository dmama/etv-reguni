package ch.vd.uniregctb.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.activation.ActivationService;
import ch.vd.uniregctb.activation.view.TiersReactivationRecapView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class TiersReactivationRecapManagerImpl implements TiersReactivationRecapManager{

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
	 * Alimente la vue TiersReactivationRecapView
	 *
	 * @param numeroTiers
	 * @return
	 */
	public TiersReactivationRecapView get(Long numeroTiers)  {
		TiersReactivationRecapView tiersReactivationRecapView = new TiersReactivationRecapView();
		Tiers tiers = tiersService.getTiers(numeroTiers);

		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(tiers);
		tiersReactivationRecapView.setTiers(tiersGeneralView);
		tiersReactivationRecapView.setDateReactivation(RegDate.get(tiers.getAnnulationDate()));

		return tiersReactivationRecapView;
	}


	/**
	 * Persiste le tiers
	 *
	 * @param tiersReactivationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(TiersReactivationRecapView tiersReactivationRecapView) {
		Tiers tiers = tiersService.getTiers(tiersReactivationRecapView.getTiers().getNumero());
		activationService.reactiveTiers(tiers, tiersReactivationRecapView.getDateReactivation());

	}

}
