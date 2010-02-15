package ch.vd.uniregctb.fusion.manager;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.fusion.view.FusionListView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service mettant a disposition des methodes pour le controller FusionListController
 *
 * @author xcifde
 *
 */
public class FusionListManagerImpl implements FusionListManager, MessageSourceAware{

	private TiersDAO tiersDAO;

	private AdresseService adresseService;

	private TiersGeneralManager tiersGeneralManager;

	private MessageSource messageSource;

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}


	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}


	public AdresseService getAdresseService() {
		return adresseService;
	}


	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}


	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}


	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}


	/**
	 * Alimente la vue FusionListView
	 *
	 * @param numeroNonHab
	 * @return une vue FusionListView
	 */
	public FusionListView get(Long numeroNonHab) {
		PersonnePhysique nonHab = (PersonnePhysique) tiersDAO.get(numeroNonHab);
		if (nonHab == null || nonHab.isHabitant()) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.non.habitant.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		FusionListView fusionListView = new FusionListView();
 		TiersGeneralView nonHabView = tiersGeneralManager.get(nonHab, true);
		fusionListView.setNonHabitant(nonHabView);
		fusionListView.setNumeroNonHabitant(numeroNonHab);
		fusionListView.setTypeRechercheDuNom(FusionListView.TypeRecherche.EST_EXACTEMENT);
		fusionListView.setTypeTiers(TiersCriteriaView.TypeTiers.HABITANT);
		return fusionListView;
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
