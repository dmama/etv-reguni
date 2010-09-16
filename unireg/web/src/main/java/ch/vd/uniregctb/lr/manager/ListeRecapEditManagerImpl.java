package ch.vd.uniregctb.lr.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.delai.DelaiDeclarationView;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.lr.view.ListeRecapDetailComparator;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.lr.view.ListeRecapListView;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service offrant des methodes pour gérer le controller ListeRecapEditController
 *
 * @author xcifde
 *
 */
public class ListeRecapEditManagerImpl implements ListeRecapEditManager, MessageSourceAware {

	protected static final Logger LOGGER = Logger.getLogger(ListeRecapEditManagerImpl.class);

	private ListeRecapitulativeDAO lrDAO;

	private PeriodeFiscaleDAO periodeFiscaleDAO;

	private ListeRecapService lrService;

	private TiersDAO tiersDAO;

	private TiersGeneralManager tiersGeneralManager;

	private TiersService tiersService;

	private EvenementFiscalService evenementFiscalService;

	private MessageSource messageSource;

	private EditiqueCompositionService editiqueCompositionService;

	private DelaisService delaisService;

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#getEvenementFiscalService()
	 */
	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#setEvenementFiscalService(ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService)
	 */
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#getLrDAO()
	 */
	public ListeRecapitulativeDAO getLrDAO() {
		return lrDAO;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#setLrDAO(ch.vd.uniregctb.declaration.ListeRecapitulativeDAO)
	 */
	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#getTiersDAO()
	 */
	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#setTiersDAO(ch.vd.uniregctb.tiers.TiersDAO)
	 */
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#getTiersService()
	 */
	public TiersService getTiersService() {
		return tiersService;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#setTiersService(ch.vd.uniregctb.tiers.TiersService)
	 */
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#getLrService()
	 */
	public ListeRecapService getLrService() {
		return lrService;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.www#setLrService(ch.vd.uniregctb.declaration.source.ListeRecapService)
	 */
	public void setLrService(ListeRecapService lrService) {
		this.lrService = lrService;
	}

	public PeriodeFiscaleDAO getPeriodeFiscaleDAO() {
		return periodeFiscaleDAO;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public DelaisService getDelaisService() {
		return delaisService;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	/**
	 * Alimente la vue ListeRecapEditView en fonction de l'ID de la LR
	 * @param id
	 * @return une vue ListeRecapEditView
	 */
	@Transactional(readOnly = true)
	public ListeRecapDetailView get(Long id) {
		ListeRecapDetailView lrEditView = new ListeRecapDetailView();
		DeclarationImpotSource lr = lrDAO.get(id);
		if (lr == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.lr.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();
		TiersGeneralView dpiView = tiersGeneralManager.get(dpi, true);
		lrEditView.setDpi(dpiView);
		EtatDeclaration etatLR = lr.getDernierEtat();
		lrEditView.setEtat(etatLR.getEtat());
		lrEditView.setImprimable(true);
		lrEditView.setId(lr.getId());
		lrEditView.setDateDebutPeriode(lr.getDateDebut());
		lrEditView.setDateFinPeriode(lr.getDateFin());
		lrEditView.setDateRetour(lr.getDateRetour());
		setDroitLR(lrEditView, dpi);
		setDelais(lrEditView, lr);
		lrEditView.setAnnule(lr.isAnnule());
		lrEditView.setImprimable(true);
		return lrEditView;
	}


	private void setDroitLR(ListeRecapDetailView lrEditView, DebiteurPrestationImposable dpi) {
		if(SecurityProvider.isGranted(Role.LR)) {
			lrEditView.setAllowedDelai(true);
		} else {
			lrEditView.setAllowedDelai(false);
		}
	}


	/**
	 * Rafraichissement de la vue
	 *
	 * @param view
	 * @return
	 */
	@Transactional(readOnly = true)
	public ListeRecapDetailView refresh(ListeRecapDetailView view) {
		if ( view.getId() == null)
			return null;
		DeclarationImpotSource lr = lrDAO.get(view.getId());
		if (lr == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.lr.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();
		TiersGeneralView dpiView = tiersGeneralManager.get(dpi, true);
		view.setDpi(dpiView);

		setDelais(view, lr);
		view.setAnnule(lr.isAnnule());
		return view;
	}

	/**
	 * Alimente la vue ListeRecapListView en fonction d'un debiteur
	 * @return une vue ListeRecapListView
	 */
	@Transactional(readOnly = true)
	public ListeRecapListView findByNumero(Long numero) {
		ListeRecapListView lrListView = new ListeRecapListView();
		TiersGeneralView tiersGeneralView = creerDpiLr(numero);

		List<ListeRecapDetailView> lrsView = new ArrayList<ListeRecapDetailView>();
		List<DeclarationImpotSource> lrs = lrDAO.findByNumero(numero);
		for (DeclarationImpotSource lr : lrs) {
			ListeRecapDetailView lrView = new ListeRecapDetailView();
			lrView.setId(lr.getId());
			lrView.setDateDebutPeriode(lr.getDateDebut());
			lrView.setDateFinPeriode(lr.getDateFin());
			lrView.setDelaiAccorde(lr.getDelaiAccordeAu());
			lrView.setDateRetour(lr.getDateRetour());
			if (lr.getDernierEtat() != null) {
				lrView.setEtat(lr.getDernierEtat().getEtat());
			}
			lrView.setAnnule(lr.isAnnule());
			lrsView.add(lrView);
		}

		Collections.sort(lrsView, new ListeRecapDetailComparator());
		lrListView.setLrs(lrsView);
		lrListView.setDpi(tiersGeneralView);
		return lrListView;
	}

	private static RegDate getProchaineDateDebutLr(RegDate dateDebutPrecedente, PeriodiciteDecompte periodicite, DebiteurPrestationImposable dpi) {
		RegDate nvelleDate = periodicite.getDebutPeriodeSuivante(dateDebutPrecedente);
		ForDebiteurPrestationImposable forDpi = dpi.getForDebiteurPrestationImposableAt(nvelleDate);
		if (forDpi == null) {
			// on essaie encore après, dès fois qu'il y aurait un trou dans les fors
			forDpi = dpi.getForDebiteurPrestationImposableAfter(nvelleDate);
			if (forDpi == null) {
				// décidément, plus rien à faire pour ce débiteur à l'avenir
				//TODO (cgd) modifier cette exception par un message d'erreur
				throw new ObjectNotFoundException("Toutes les LR de ce débiteur ont déjà été émises.");
			}
			else {
				nvelleDate = periodicite.getDebutPeriode(forDpi.getDateDebut());
			}
		}
		return nvelleDate;
	}

	/**
	 * Cree une nouvelle LR
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public ListeRecapDetailView creerLr(Long numero) {
		final ListeRecapDetailView lrEditView = new ListeRecapDetailView();
		lrEditView.setDpi(creerDpiLr(numero));
		final EtatDeclaration etatPeriode = lrDAO.findDerniereLrEnvoyee(numero);
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(numero);
		final RegDate dateDebutActivite = dpi.getDateDebutActivite();

		final RegDate aujourdhui = RegDate.get();

		// Créée une LR apres la courante
		if (etatPeriode != null) {
			final DeclarationImpotSource lr = (DeclarationImpotSource) etatPeriode.getDeclaration();
			dpi = (DebiteurPrestationImposable) lr.getTiers();
			if  (PeriodiciteDecompte.UNIQUE == dpi.getPeriodiciteAt(aujourdhui).getPeriodiciteDecompte()) {
				setLRViewPeriodiciteUnique(lrEditView, lr, dpi);
			}
			else {
				final List<DateRange> lrTrouvees = new ArrayList<DateRange>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, aujourdhui, lrTrouvees);
				final DateRange periodeInteressante = new DateRangeHelper.Range(null, aujourdhui);
				if (lrManquantes != null && lrManquantes.size() > 0 && periodeInteressante.isValidAt(lrManquantes.get(0).getDateFin()) &&  !DateRangeHelper.intersect(lrManquantes.get(0), lrTrouvees)) {
					lrEditView.setDateDebutPeriode(lrManquantes.get(0).getDateDebut());
					lrEditView.setDateFinPeriode(lrManquantes.get(0).getDateFin());
					lrEditView.setPeriodicite(dpi.getPeriodiciteAt(lrManquantes.get(0).getDateDebut()).getPeriodiciteDecompte());
				}
				else {
					final PeriodiciteDecompte periodicite = getPeriodicite(lr, lr.getDateDebut());
					final RegDate nouvDateDebut = getProchaineDateDebutLr(lr.getDateDebut(), periodicite, dpi);
					lrEditView.setDateDebutPeriode(nouvDateDebut);
					lrEditView.setDateFinPeriode(periodicite.getFinPeriode(nouvDateDebut));
					lrEditView.setPeriodicite(periodicite);
				}
			}
		}
		// Créée une premiere LR sur ce debiteur
		else {
			final PeriodiciteDecompte periodicite = dpi.getPeriodiciteAt(RegDate.get()).getPeriodiciteDecompte();
			if (PeriodiciteDecompte.UNIQUE == periodicite) {
				setLRViewPeriodiciteUnique(lrEditView, null, dpi);
			}
			else {
				final RegDate nouvDateDebut = periodicite.getDebutPeriode(dateDebutActivite);
				lrEditView.setImprimable(true);
				lrEditView.setDateDebutPeriode(nouvDateDebut);
				lrEditView.setPeriodicite(periodicite);
				lrEditView.setDateFinPeriode(periodicite.getFinPeriode(nouvDateDebut));
			}
		}
		lrEditView.setSansSommation(dpi.getSansRappel());
		lrEditView.setImprimable(true);
		lrEditView.setDelaiAccorde(delaisService.getDateFinDelaiRetourListeRecapitulative(aujourdhui, lrEditView.getRegDateFinPeriode()));
		setDroitLR(lrEditView, dpi);
		return lrEditView;
	}


	private void setLRViewPeriodiciteUnique(ListeRecapDetailView lrEditView, DeclarationImpotSource lr, DebiteurPrestationImposable dpi) {
		PeriodiciteDecompte periodicite = PeriodiciteDecompte.UNIQUE;
		RegDate nouvDateDebut = null;
		RegDate nouvDateFin = null;
		RegDate precDateFin = null;
		if (lr != null) {
			precDateFin = lr.getDateFin();
		}
		else {
			RegDate dateDebutActivite = dpi.getDateDebutActivite();
			precDateFin = dateDebutActivite.addDays(-1);
		}
		int precMoisFin = precDateFin.month();
		int precAnneeFin = precDateFin.year();
		if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M01)) {
			nouvDateDebut = RegDate.get(precAnneeFin + 1, 1, 1);
			nouvDateFin = RegDate.get(precAnneeFin + 1, 1, 31);
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M02)) {
			if (precMoisFin >= 2) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 2, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 3, 1);
				nouvDateFin = nouvDateFin.addDays(-1);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 2, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 3, 1);
				nouvDateFin = nouvDateFin.addDays(-1);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M03)) {
			if (precMoisFin >= 3) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 3, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 3, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 3, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 3, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M04)) {
			if (precMoisFin >= 4) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 4, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 4, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 4, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 4, 30);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M05)) {
			if (precMoisFin >= 5) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 5, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 5, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 5, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 5, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M06)) {
			if (precMoisFin >= 6) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 6, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 6, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 6, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 6, 30);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M07)) {
			if (precMoisFin >= 7) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 7, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 7, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M08)) {
			if (precMoisFin >= 8) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 8, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 8, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 8, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 8, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M09)) {
			if (precMoisFin >= 9) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 9, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 9, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 9, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 9, 30);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M10)) {
			if (precMoisFin >= 10) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 10, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 10, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 10, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 10, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M11)) {
			if (precMoisFin >= 11) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 11, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 11, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 11, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 11, 30);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.M12)) {
			if (precMoisFin == 12) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 12, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 12, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 12, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 12, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.T1)) {
			nouvDateDebut = RegDate.get(precAnneeFin + 1, 1, 1);
			nouvDateFin = RegDate.get(precAnneeFin + 1, 3, 31);
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.T2)) {
			if (precMoisFin >= 3) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 4, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 6, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 4, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 6, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.T3)) {
			if (precMoisFin >= 6) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 9, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 9, 30);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.T4)) {
			if (precMoisFin >= 9) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 10, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 12, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 10, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 10, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.S1)) {
			nouvDateDebut = RegDate.get(precAnneeFin + 1, 1, 1);
			nouvDateFin = RegDate.get(precAnneeFin + 1, 6, 30);
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.S2)) {
			if (precMoisFin >= 6) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 12, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 12, 31);
			}
		}
		else if (dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte().equals(PeriodeDecompte.A)) {
			nouvDateDebut = RegDate.get(precAnneeFin + 1, 1, 1);
			nouvDateFin = RegDate.get(precAnneeFin + 1, 12, 31);
		}
		lrEditView.setDateDebutPeriode(nouvDateDebut);
		lrEditView.setPeriodicite(periodicite);
		lrEditView.setDateFinPeriode(nouvDateFin);
	}

	/**
	 * Détermine la périodicité à appliquer à la LR
	 *
	 * @param lr
	 * @param dateDebut
	 * @return
	 */
	private PeriodiciteDecompte getPeriodicite(DeclarationImpotSource lr, RegDate dateDebut) {

		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();
		PeriodiciteDecompte periodiciteDPI = dpi.getPeriodiciteAt(lr.getDateDebut()).getPeriodiciteDecompte();
		PeriodiciteDecompte periodiciteLR = lr.getPeriodicite();
		Assert.notNull(periodiciteLR);

		final RegDate nouvelleDateDebut = periodiciteLR.getDebutPeriodeSuivante(dateDebut);
		final int month = nouvelleDateDebut.month();

		if (!periodiciteDPI.equals(periodiciteLR)) {
			if (		periodiciteDPI.equals(PeriodiciteDecompte.TRIMESTRIEL)
					&& 	periodiciteLR.equals(PeriodiciteDecompte.MENSUEL)
					&&		(month != 1) && (month != 4)
						&& 	(month != 7) && (month != 10) ) {
				return periodiciteLR;
			}
			if (		periodiciteDPI.equals(PeriodiciteDecompte.SEMESTRIEL)
					&& 	(		periodiciteLR.equals(PeriodiciteDecompte.MENSUEL)
							|| 	periodiciteLR.equals(PeriodiciteDecompte.TRIMESTRIEL))
					&&		(month != 1) && (month != 7)  ) {
				return periodiciteLR;
			}
			if (		periodiciteDPI.equals(PeriodiciteDecompte.ANNUEL)
					&& 	(		periodiciteLR.equals(PeriodiciteDecompte.MENSUEL)
							|| 	periodiciteLR.equals(PeriodiciteDecompte.TRIMESTRIEL)
							|| 	periodiciteLR.equals(PeriodiciteDecompte.SEMESTRIEL))
					&&	month != 1) {
				return periodiciteLR;
			}
		}

		return periodiciteDPI;
	}


	/**
	 * Cree le debiteur pour la LR
	 *
	 * @param numero
	 * @return
	 */
	private TiersGeneralView creerDpiLr(Long numero) {
		Tiers tiers = tiersDAO.get(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
		TiersGeneralView dpiView = tiersGeneralManager.get(dpi, true);
		return dpiView;
	}

	/**
	 * Persiste en base et indexe le tiers modifie
	 *
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public DeclarationImpotSource save(ListeRecapDetailView lrEditView) {
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(lrEditView.getDpi().getNumero());

		EtatDeclaration etat = null;
		DeclarationImpotSource lr = null;
		if (lrEditView.getId() == null)	{
			lr = new DeclarationImpotSource();
			etat = new EtatDeclaration();
			etat.setEtat(TypeEtatDeclaration.EMISE);
			etat.setDateObtention(RegDate.get());
			lr.addEtat(etat);
			lr.setDateDebut(RegDate.get(lrEditView.getDateDebutPeriode()));
			lr.setDateFin(RegDate.get(lrEditView.getDateFinPeriode()));
			lr.setModeCommunication(dpi.getModeCommunication());
			lr.setPeriodicite(lrEditView.getPeriodicite());
			int annee = RegDate.get(lrEditView.getDateDebutPeriode()).year();
			PeriodeFiscale periodeFiscale = periodeFiscaleDAO.getPeriodeFiscaleByYear(annee);
			if (periodeFiscale == null) {
				throw new ValidationException(lr, "Veuillez initialiser la période fiscale correspondante.");
			}
			lr.setPeriode(periodeFiscale);
			lr.setSansRappel(lrEditView.getSansSommation());

			//DelaiDeclaration delai = new DelaiDeclaration();
			//delai.setDateDemande(theDateDemande);
			//delai.setDateTraitement(RegDate.get());
			//TODO (FNR) décaler date du délai si samedi, dimanche ou jours férié
			//delai.setDelaiAccordeAu(lr.getDateFin().addMonths(1));

			DelaiDeclaration delai = new DelaiDeclaration();
			delai.setDelaiAccordeAu(lrEditView.getRegDelaiAccorde());
			delai.setDateDemande(RegDate.get());
			delai.setDateTraitement(RegDate.get());
			lr.addDelai(delai);
			lr.setTiers(dpi);
			lr = lrDAO.save(lr);
			dpi.addDeclaration(lr);
			//tiersDAO.save(dpi);
			evenementFiscalService.publierEvenementFiscalOuverturePeriodeDecompteLR(dpi, lr, RegDate.get());
		}
		else {
			lr = lrDAO.get(lrEditView.getId());
			if (lrEditView.getDateRetour() != null) {
				if (!lrEditView.getDateRetour().equals(lr.getDateRetour())) {
					etat = new EtatDeclaration();
					etat.setEtat(TypeEtatDeclaration.RETOURNEE);
					etat.setDateObtention(RegDate.get(lrEditView.getDateRetour()));
					lr.addEtat(etat);
					evenementFiscalService.publierEvenementFiscalRetourLR(dpi, lr, RegDate.get(lrEditView.getDateRetour()));
				}
			}
		}

		return lr;
	}

	/**
	 * Annule un delai
	 *
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerDelai (ListeRecapDetailView lrEditView, Long idDelai) {
		DeclarationImpotSource lr = lrDAO.get(lrEditView.getId());
		Set<DelaiDeclaration> delais = lr.getDelais();
		for (DelaiDeclaration delai : delais) {
			if (delai.getId().equals(idDelai)) {
				delai.setAnnule(true);
			}
		}
	}

	/**
	 * Persiste en base et indexe le tiers modifie
	 *
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void saveDelai (Long idLr, DelaiDeclaration delai) {

		DeclarationImpotSource lr = lrDAO.get(idLr);
		Set<DelaiDeclaration> delais = lr.getDelais();
		if (delais == null ) {
			delais = new HashSet<DelaiDeclaration>();
		}
		delai.setDateTraitement(RegDate.get());
		delai.setAnnule(false);
		delais.add(delai);
		lr.setDelais(delais);
	}

	/**
	 * Contrôle la présence de la LR
	 *
	 * @param id
	 */
	@Transactional(readOnly = true)
	public void controleLR(Long id) {
		DeclarationImpotSource lr = lrDAO.get(id);
		if (lr == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.lr.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
	}

	private void setDelais(ListeRecapDetailView lrEditView, DeclarationImpotSource lr) {
		List<DelaiDeclarationView> delaisView = new ArrayList<DelaiDeclarationView>();
		for (DelaiDeclaration delai : lr.getDelais()) {
			DelaiDeclarationView delaiView = new DelaiDeclarationView();
			delaiView.setId(delai.getId());
			delaiView.setAnnule(delai.isAnnule());
			delaiView.setConfirmationEcrite(delai.getConfirmationEcrite());
			delaiView.setDateDemande(delai.getDateDemande());
			delaiView.setDateTraitement(delai.getDateTraitement());
			delaiView.setDelaiAccordeAu(delai.getDelaiAccordeAu());
			delaiView.setLogModifDate(delai.getLogModifDate());
			delaiView.setLogModifUser(delai.getLogModifUser());
			if (lr.getPremierDelai().equals(delai.getDelaiAccordeAu())) {
				delaiView.setFirst(true);
			}
			else {
				delaiView.setFirst(false);
			}
			delaisView.add(delaiView);
		}
		Collections.sort(delaisView);
		lrEditView.setDelais(delaisView);
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

	public EditiqueResultat envoieImpressionLocalLR(ListeRecapDetailView lrEditView) throws EditiqueException {
		//Sauvegarde de la DI
		final DeclarationImpotSource lr = save(lrEditView);
		lrEditView.setImprimable(false);

		try {
			//Envoi du flux xml à l'éditique
			return editiqueCompositionService.imprimeLROnline(lr, RegDate.get(), TypeDocument.LISTE_RECAPITULATIVE);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}


	public EditiqueResultat envoieImpressionLocalSommationLR(ListeRecapDetailView lrEditView) throws EditiqueException {
		//Sauvegarde de la DI
		final DeclarationImpotSource lr = save(lrEditView);
		final EtatDeclaration etat = new EtatDeclaration();
		etat.setEtat(TypeEtatDeclaration.SOMMEE);
		etat.setDateObtention(RegDate.get());
		lr.addEtat(etat);
		lrDAO.save(lr);

		try {
			//Envoi du flux xml à l'éditique
			return editiqueCompositionService.imprimeSommationLROnline(lr, RegDate.get());
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@Transactional(readOnly = true)
	public EditiqueResultat envoieImpressionLocalDuplicataLR(ListeRecapDetailView lrEditView) throws EditiqueException {
		try {
			final DeclarationImpotSource lr = lrDAO.get(lrEditView.getId());

			Audit.info(String.format("Impression d'un duplicata de LR pour le débiteur %d et la période [%s ; %s]",
									lr.getTiers().getNumero(),
									RegDateHelper.dateToDashString(lr.getDateDebut()),
									RegDateHelper.dateToDashString(lr.getDateFin())));
			
			return editiqueCompositionService.imprimeLROnline(lr, RegDate.get(), TypeDocument.LISTE_RECAPITULATIVE);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	/**
	 * Annule une LR
	 *
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerLR (ListeRecapDetailView lrEditView) {
		DeclarationImpotSource lr = lrDAO.get(lrEditView.getId());
		lr.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationLR((DebiteurPrestationImposable) lr.getTiers(), lr, RegDate.get()) ;
	}

}
