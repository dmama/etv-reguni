package ch.vd.unireg.fusion.manager;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.fusion.view.FusionRecapView;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.utils.WebContextUtils;

/**
 * Methodes pour gerer FusionRecapController
 *
 * @author xcifde
 *
 */
public class FusionRecapManagerImpl implements FusionRecapManager, MessageSourceAware{

	private TiersGeneralManager tiersGeneralManager;

	private TiersService tiersService;

	private MessageSource messageSource;

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
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
	@Override
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
	@Override
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

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}