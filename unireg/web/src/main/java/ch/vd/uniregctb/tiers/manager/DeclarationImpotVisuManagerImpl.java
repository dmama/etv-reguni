package ch.vd.uniregctb.tiers.manager;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.individu.HostCivilService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service qui fournit les methodes pour visualiser une declaration d'impot
 *
 * @author xcifde
 *
 */
public class DeclarationImpotVisuManagerImpl implements DeclarationImpotVisuManager, MessageSourceAware {

	private TiersGeneralManager tiersGeneralManager;

	private TiersDAO tiersDAO ;

	private DeclarationImpotOrdinaireDAO diDAO;

	private HostCivilService hostCivilService;

	private MessageSource messageSource;

	/**
	 * Charge les informations dans DeclarationImpotOrdinaireView
	 *
	 * @param numero
	 * @return un objet DeclarationImpotOrdinaireView
	 */
	@Override
	@Transactional(readOnly = true)
	public DeclarationImpotDetailView get(Long numero) {
		DeclarationImpotDetailView diView = new DeclarationImpotDetailView();
		DeclarationImpotOrdinaire di = diDAO.get(numero);

		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(di.getTiers(), true);
		diView.setContribuable(tiersGeneralView);
		diView.fill(di);

		return diView;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public DeclarationImpotOrdinaireDAO getDiDAO() {
		return diDAO;
	}

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
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
