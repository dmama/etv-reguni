package ch.vd.uniregctb.fusion.manager;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.fusion.view.FusionRecapView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.individu.HostCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Methodes pour gerer FusionRecapController
 *
 * @author xcifde
 *
 */
public class FusionRecapManagerImpl implements FusionRecapManager, MessageSourceAware{

	private TiersGeneralManager tiersGeneralManager;

	private TiersService tiersService;

	private AdresseService adresseService;

	private HostCivilService hostCivilService;

	private MessageSource messageSource;

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public HostCivilService getHostCivilService() {
		return hostCivilService;
	}


	public void setHostCivilService(HostCivilService hostCivilService) {
		this.hostCivilService = hostCivilService;
	}


	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	/**
	 * Alimente la vue FusionRecapView
	 *
	 * @param numeroNonHabitant
	 * @param numeroHabitant
	 * @return
	 */
	@Transactional(readOnly = true)
	public FusionRecapView get (Long numeroNonHabitant, Long numeroHabitant) {
		FusionRecapView fusionRecapView =  new FusionRecapView();


		PersonnePhysique nonHabitant = (PersonnePhysique) tiersService.getTiers(numeroNonHabitant);
		if (nonHabitant == null || nonHabitant.isHabitantVD()) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.non.habitant.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		TiersGeneralView nonHabitantView = tiersGeneralManager.getPersonnePhysique(nonHabitant, true);
		fusionRecapView.setNonHabitant(nonHabitantView);

		PersonnePhysique habitant = (PersonnePhysique) tiersService.getTiers(numeroHabitant);
		if (habitant == null || !habitant.isHabitantVD()) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.habitant.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		TiersGeneralView habitantView = tiersGeneralManager.getPersonnePhysique(habitant, true);

		fusionRecapView.setHabitant(habitantView);

		return fusionRecapView;
	}


	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(FusionRecapView fusionRecapView) {
		PersonnePhysique nonHabitant = (PersonnePhysique) tiersService.getTiers(fusionRecapView.getNonHabitant().getNumero());
		PersonnePhysique habitant = (PersonnePhysique) tiersService.getTiers(fusionRecapView.getHabitant().getNumero());
		Assert.notNull(nonHabitant);
		Assert.notNull(habitant);
		tiersService.fusionne(habitant, nonHabitant);
	}

	/**
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
