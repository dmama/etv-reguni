package ch.vd.uniregctb.activation.manager;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.activation.ActivationService;
import ch.vd.uniregctb.activation.ActivationServiceException;
import ch.vd.uniregctb.activation.view.TiersAnnulationRecapView;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class TiersAnnulationRecapManagerImpl implements TiersAnnulationRecapManager {

	public static final Logger LOGGER = Logger.getLogger(TiersAnnulationRecapManagerImpl.class);

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
	@Transactional(readOnly = true)
	public TiersAnnulationRecapView get(Long numeroTiers)  {

		final TiersAnnulationRecapView tiersAnnulationRecapView = new TiersAnnulationRecapView();
		final Tiers tiers = tiersService.getTiers(numeroTiers);
		tiersAnnulationRecapView.setTypeTiers(tiers.getType());

		final TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
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
	@Transactional(readOnly = true)
	public TiersAnnulationRecapView get(Long numeroTiers, Long numeroTiersRemplacant)  {

		final TiersAnnulationRecapView tiersAnnulationRecapView = new TiersAnnulationRecapView();
		final Tiers tiers = tiersService.getTiers(numeroTiers);
		tiersAnnulationRecapView.setTypeTiers(tiers.getType());

		final TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
		tiersAnnulationRecapView.setTiers(tiersGeneralView);

		final Tiers tiersRemplacant = tiersService.getTiers(numeroTiersRemplacant);
		final TiersGeneralView tiersRemplacantGeneralView = tiersGeneralManager.getTiers(tiersRemplacant, true);
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

		try {
			final Tiers tiers = tiersService.getTiers(tiersAnnulationRecapView.getTiers().getNumero());
			if (tiersAnnulationRecapView.getTiersRemplacant() != null) {
				final Tiers tiersRemplacant = tiersService.getTiers(tiersAnnulationRecapView.getTiersRemplacant().getNumero());
				activationService.remplaceTiers(tiers, tiersRemplacant, tiersAnnulationRecapView.getDateAnnulation());
			}
			else {
				activationService.desactiveTiers(tiers, tiersAnnulationRecapView.getDateAnnulation());
			}
		}
		catch (ActivationServiceException e) {
			LOGGER.error("L'opération d'annulation/remplacement de tiers a échoué", e);
			throw new ActionException(e.getMessage());
		}
	}
}
