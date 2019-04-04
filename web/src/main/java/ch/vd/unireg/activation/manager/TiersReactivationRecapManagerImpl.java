package ch.vd.unireg.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.activation.ActivationDesactivationHelper;
import ch.vd.unireg.activation.ActivationService;
import ch.vd.unireg.activation.ActivationServiceException;
import ch.vd.unireg.activation.view.TiersReactivationRecapView;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

public class TiersReactivationRecapManagerImpl implements TiersReactivationRecapManager{

	private TiersGeneralManager tiersGeneralManager;
	private TiersService tiersService;
	private ActivationService activationService;
	private SecurityProviderInterface securityProvider;

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setActivationService(ActivationService activationService) {
		this.activationService = activationService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	/**
	 * Alimente la vue TiersReactivationRecapView
	 *
	 * @param numeroTiers
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersReactivationRecapView get(Long numeroTiers)  {
		final TiersReactivationRecapView tiersReactivationRecapView = new TiersReactivationRecapView();
		final Tiers tiers = tiersService.getTiers(numeroTiers);

		final TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
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
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(TiersReactivationRecapView tiersReactivationRecapView) throws ActivationServiceException {
		final Tiers tiers = tiersService.getTiers(tiersReactivationRecapView.getTiers().getNumero());

		final boolean droitOk = ActivationDesactivationHelper.isActivationDesactivationAllowed(tiers.getNatureTiers(), securityProvider);
		if (!droitOk) {
			throw new AccessDeniedException("Vous ne possédez par les droits de ré-activation des tiers de nature " + tiers.getNatureTiers());
		}

		activationService.reactiveTiers(tiers, tiersReactivationRecapView.getDateReactivation());
	}

}
