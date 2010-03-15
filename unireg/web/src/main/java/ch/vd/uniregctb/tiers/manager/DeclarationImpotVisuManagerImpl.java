package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.delai.DelaiDeclarationView;
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
	public DeclarationImpotDetailView get(Long numero) {
		DeclarationImpotDetailView diView = new DeclarationImpotDetailView();
		DeclarationImpotOrdinaire di = diDAO.get(numero);

		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(di.getTiers());
		diView.setContribuable(tiersGeneralView);
		diView.setDateDebutPeriodeImposition(di.getDateDebut());
		diView.setDateFinPeriodeImposition(di.getDateFin());
		diView.setPeriodeFiscale(di.getPeriode().getAnnee());
		diView.setTypeDeclarationImpot(di.getTypeDeclaration());
		List<DelaiDeclarationView> delaisView = new ArrayList<DelaiDeclarationView>();
		for (DelaiDeclaration delai : di.getDelais()) {
			DelaiDeclarationView delaiView = new DelaiDeclarationView();
			delaiView.setId(delai.getId());
			delaiView.setAnnule(delai.isAnnule());
			delaiView.setConfirmationEcrite(delai.getConfirmationEcrite());
			delaiView.setDateDemande(delai.getDateDemande());
			delaiView.setDateTraitement(delai.getDateTraitement());
			delaiView.setDelaiAccordeAu(delai.getDelaiAccordeAu());
			delaiView.setLogModifDate(delai.getLogModifDate());
			delaiView.setLogModifUser(delai.getLogModifUser());
			if (di.getPremierDelai() != null && di.getPremierDelai().equals(delai.getDelaiAccordeAu())) {
				delaiView.setFirst(true);
			}
			else {
				delaiView.setFirst(false);
			}
			delaisView.add(delaiView);
		}
		Collections.sort(delaisView);
		diView.setDelais(delaisView);
		Collections.sort(diView.getDelais());
		diView.setEtats(new ArrayList<EtatDeclaration>(di.getEtats()));
		Collections.sort(diView.getEtats());
		diView.setId(di.getId());

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

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;

	}

}
