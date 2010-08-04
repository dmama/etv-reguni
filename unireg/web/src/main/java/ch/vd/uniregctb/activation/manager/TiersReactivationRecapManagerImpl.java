package ch.vd.uniregctb.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.activation.ActivationService;
import ch.vd.uniregctb.activation.ActivationServiceException;
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
	@Transactional(readOnly = true)
	public TiersReactivationRecapView get(Long numeroTiers)  {
		final TiersReactivationRecapView tiersReactivationRecapView = new TiersReactivationRecapView();
		final Tiers tiers = tiersService.getTiers(numeroTiers);

		final TiersGeneralView tiersGeneralView = tiersGeneralManager.get(tiers);
		tiersReactivationRecapView.setTiers(tiersGeneralView);
		final RegDate dateDesactivation = tiers.getDateDesactivation();
		tiersReactivationRecapView.setDateReactivation(dateDesactivation != null ? dateDesactivation.getOneDayAfter() : null);

		return tiersReactivationRecapView;
	}


	/**
	 * Persiste le tiers
	 *
	 * @param tiersReactivationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(TiersReactivationRecapView tiersReactivationRecapView) throws ActivationServiceException {
		final Tiers tiers = tiersService.getTiers(tiersReactivationRecapView.getTiers().getNumero());
		activationService.reactiveTiers(tiers, tiersReactivationRecapView.getDateReactivation());
	}

}
