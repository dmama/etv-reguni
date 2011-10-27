package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.delai.DelaiDeclarationView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.individu.HostCivilService;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service qui fournit les methodes pour visualiser une liste recapitulative
 *
 * @author xcifde
 *
 */
public class ListeRecapVisuManagerImpl implements ListeRecapVisuManager,MessageSourceAware {

	private TiersDAO tiersDAO ;

	private TiersGeneralManager tiersGeneralManager;

	private ListeRecapitulativeDAO lrDAO;

	private HostCivilService hostCivilService;

	private MessageSource messageSource;

	protected final Logger LOGGER = Logger.getLogger(ListeRecapVisuManagerImpl.class);

	/**
	 * Charge les informations dans ListeRecapitulativeView
	 *
	 * @param numero
	 * @return un objet ListeRecapitulativeView
	 * @throws AdressesResolutionException
	 */
	@Override
	@Transactional(readOnly = true)
	public ListeRecapDetailView get(Long numero) {
		ListeRecapDetailView lrView = new ListeRecapDetailView();
		DeclarationImpotSource lr = lrDAO.get(numero);
		if (lr == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.lr.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		TiersGeneralView tiersGeneralView = tiersGeneralManager.getDebiteur((DebiteurPrestationImposable)lr.getTiers(), true);
		lrView.setDpi(tiersGeneralView);
		lrView.setDateDebutPeriode(lr.getDateDebut());
		lrView.setDateFinPeriode(lr.getDateFin());
		List<DelaiDeclarationView> delaisView = new ArrayList<DelaiDeclarationView>();
		for (DelaiDeclaration delai : lr.getDelais()) {
			DelaiDeclarationView delaiView = new DelaiDeclarationView(delai);
			delaiView.setFirst(lr.getPremierDelai() == delai.getDelaiAccordeAu());
			delaisView.add(delaiView);
		}
		Collections.sort(delaisView);
		lrView.setDelais(delaisView);
		lrView.setEtats(new ArrayList<EtatDeclaration>(lr.getEtats()));
		Collections.sort(lrView.getEtats());
		lrView.setId(lr.getId());
		Tiers tiers = tiersDAO.get(lr.getTiers().getId());
		lrView.setNumero(tiers.getNumero());

		return lrView;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public ListeRecapitulativeDAO getLrDAO() {
		return lrDAO;
	}

	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
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
