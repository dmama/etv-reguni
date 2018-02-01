package ch.vd.uniregctb.activation.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.activation.ActivationDesactivationHelper;
import ch.vd.uniregctb.activation.ActivationService;
import ch.vd.uniregctb.activation.ActivationServiceException;
import ch.vd.uniregctb.activation.view.TiersAnnulationRecapView;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class TiersAnnulationRecapManagerImpl implements TiersAnnulationRecapManager {

	public static final Logger LOGGER = LoggerFactory.getLogger(TiersAnnulationRecapManagerImpl.class);

	private TiersService tiersService;
	private ActivationService activationService;
	private SecurityProviderInterface securityProvider;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setActivationService(ActivationService activationService) {
		this.activationService = activationService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	/**
	 * Alimente la vue TiersAnnulationRecapView
	 *
	 * @param numeroTiers
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersAnnulationRecapView get(Long numeroTiers)  {

		final TiersAnnulationRecapView tiersAnnulationRecapView = new TiersAnnulationRecapView();
		final Tiers tiers = tiersService.getTiers(numeroTiers);
		tiersAnnulationRecapView.setTypeTiers(tiers.getType());
		tiersAnnulationRecapView.setNumeroTiers(numeroTiers);

		return tiersAnnulationRecapView;
	}

	/**
	 * Alimente la vue TiersAnnulationRecapView
	 *
	 * @param numeroTiers
	 * @param numeroTiersRemplacant
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersAnnulationRecapView get(Long numeroTiers, Long numeroTiersRemplacant)  {

		final TiersAnnulationRecapView tiersAnnulationRecapView = new TiersAnnulationRecapView();
		final Tiers tiers = tiersService.getTiers(numeroTiers);
		tiersAnnulationRecapView.setTypeTiers(tiers.getType());
		tiersAnnulationRecapView.setNumeroTiers(numeroTiers);
		tiersAnnulationRecapView.setNumeroTiersRemplacant(numeroTiersRemplacant);
		return tiersAnnulationRecapView;
	}


	/**
	 * Persiste le tiers
	 *
	 * @param tiersAnnulationRecapView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(TiersAnnulationRecapView tiersAnnulationRecapView) {

		try {
			final Tiers tiers = tiersService.getTiers(tiersAnnulationRecapView.getNumeroTiers());

			final boolean droitOk = ActivationDesactivationHelper.isActivationDesactivationAllowed(tiers.getNatureTiers(), securityProvider);
			if (!droitOk) {
				throw new AccessDeniedException("Vous ne possédez pas les droit IfoSec d'annulation/désactivation des tiers de nature " + tiers.getNatureTiers());
			}

			if (tiersAnnulationRecapView.getNumeroTiersRemplacant() != null) {
				final Tiers tiersRemplacant = tiersService.getTiers(tiersAnnulationRecapView.getNumeroTiersRemplacant());
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
