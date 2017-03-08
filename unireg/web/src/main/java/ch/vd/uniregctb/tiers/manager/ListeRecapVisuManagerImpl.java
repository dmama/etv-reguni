package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.view.DelaiDeclarationView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
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
public class ListeRecapVisuManagerImpl implements ListeRecapVisuManager, MessageSourceAware {

	private TiersDAO tiersDAO ;
	private TiersGeneralManager tiersGeneralManager;
	private ListeRecapitulativeDAO lrDAO;
	private MessageSource messageSource;
	private ServiceInfrastructureService infraService;

	protected final Logger LOGGER = LoggerFactory.getLogger(ListeRecapVisuManagerImpl.class);

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
		List<DelaiDeclarationView> delaisView = new ArrayList<>();
		for (DelaiDeclaration delai : lr.getDelais()) {
			DelaiDeclarationView delaiView = new DelaiDeclarationView(delai, infraService, getMessageSource());
			delaiView.setFirst(lr.getPremierDelai() == delai.getDelaiAccordeAu());
			delaisView.add(delaiView);
		}
		Collections.sort(delaisView);
		lrView.setDelais(delaisView);
		lrView.setEtats(new ArrayList<>(lr.getEtats()));
		Collections.sort(lrView.getEtats());
		lrView.setId(lr.getId());
		Tiers tiers = tiersDAO.get(lr.getTiers().getId());
		lrView.setNumero(tiers.getNumero());

		return lrView;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
	}

	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
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
