package ch.vd.uniregctb.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.activation.ActivationService;
import ch.vd.uniregctb.activation.ActivationServiceException;
import ch.vd.uniregctb.activation.view.TiersReactivationRecapView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

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

		final boolean droitOk;
		final NatureTiers nature = tiers.getNatureTiers();
		if (nature == NatureTiers.Etablissement || nature == NatureTiers.Entreprise) {
			droitOk = SecurityHelper.isGranted(securityProvider, Role.MODIF_PM);
		}
		else {
			droitOk = SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}
		if (!droitOk) {
			throw new AccessDeniedException("Vous ne possédez pas les droit IfoSec de ré-activation des tiers de nature " + nature);
		}

		activationService.reactiveTiers(tiers, tiersReactivationRecapView.getDateReactivation());
	}

}
