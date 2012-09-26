package ch.vd.uniregctb.lr.manager;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.di.view.DelaiDeclarationView;
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
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeDocument;
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
	private SecurityProviderInterface securityProvider;

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setLrService(ListeRecapService lrService) {
		this.lrService = lrService;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	/**
	 * Alimente la vue ListeRecapEditView en fonction de l'ID de la LR
	 * @param id
	 * @return une vue ListeRecapEditView
	 */
	@Override
	@Transactional(readOnly = true)
	public ListeRecapDetailView get(Long id) {
		ListeRecapDetailView lrEditView = new ListeRecapDetailView();
		DeclarationImpotSource lr = lrDAO.get(id);
		if (lr == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.lr.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();
		TiersGeneralView dpiView = tiersGeneralManager.getDebiteur(dpi, true);
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
		if(SecurityHelper.isGranted(securityProvider, Role.LR)) {
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
	@Override
	@Transactional(readOnly = true)
	public ListeRecapDetailView refresh(ListeRecapDetailView view) {
		if ( view.getId() == null)
			return null;
		DeclarationImpotSource lr = lrDAO.get(view.getId());
		if (lr == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.lr.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();
		TiersGeneralView dpiView = tiersGeneralManager.getDebiteur(dpi, true);
		view.setDpi(dpiView);

		setDelais(view, lr);
		view.setAnnule(lr.isAnnule());
		return view;
	}

	/**
	 * Alimente la vue ListeRecapListView en fonction d'un debiteur
	 * @return une vue ListeRecapListView
	 */
	@Override
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

	@Override
	public DelaiDeclarationView creerDelai(Long idLr) {
		DelaiDeclarationView  delaiView = new DelaiDeclarationView();
		delaiView.setIdDeclaration(idLr);
		DeclarationImpotSource lr = lrDAO.get(idLr);
		if (lr == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.lr.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		delaiView.setTiersId(lr.getTiers().getId());
		delaiView.setDeclarationPeriode(lr.getPeriode().getAnnee());
		delaiView.setDeclarationRange(new DateRangeHelper.Range(lr.getDateDebut(), lr.getDateFin()));
		delaiView.setDateExpedition(lr.getDateExpedition());
		delaiView.setOldDelaiAccorde(lr.getDelaiAccordeAu());
		delaiView.setAnnule(false);
		delaiView.setDateDemande(RegDate.get());

		return delaiView;
	}

	/**
	 * Cree une nouvelle LR
	 *
	 * @param numero
	 * @return
	 */
	@Override
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
			final List<DateRange> lrTrouvees = new ArrayList<DateRange>();
			final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, aujourdhui, lrTrouvees);
			final DateRange periodeInteressante = new DateRangeHelper.Range(null, aujourdhui);
			//[UNIREG-3120] la LR à ajouter doit recouper au moins une période d'activité du débiteur
			if (lrManquantes != null && !lrManquantes.isEmpty()
					&& (periodeInteressante.isValidAt(lrManquantes.get(0).getDateFin()) || periodeInteressante.isValidAt(lrManquantes.get(0).getDateDebut()))
					&& !DateRangeHelper.intersect(lrManquantes.get(0), lrTrouvees)) {
				lrEditView.setDateDebutPeriode(lrManquantes.get(0).getDateDebut());
				lrEditView.setDateFinPeriode(lrManquantes.get(0).getDateFin());
				//[UNIREG-3115] Periodicite non trouvé en debut de periode de lR on cherche à la fin.
				Periodicite periodiciteAt = dpi.findPeriodicite(lrManquantes.get(0).getDateDebut(),lrManquantes.get(0).getDateFin());
				lrEditView.setPeriodicite(periodiciteAt.getPeriodiciteDecompte());
			}
			else {

				final Periodicite periodiciteSuivante = getPeriodiciteSuivante(dpi,lr);
				if (periodiciteSuivante == null) {
					throw new IllegalArgumentException("Incohérence des données : le débiteur n°" + dpi.getNumero() +
							" ne possède pas de périodicité à la date [" + RegDateHelper.dateToDisplayString(lr.getDateFin()) +
							"] alors même qu'il existe un for fiscal. Problème de création des périodicités ?");

				}

				DateRange periodeSuivante = getPeriodeSuivante(dpi, periodiciteSuivante,lr);
				//UNIREG-3120 - 2 On ne doit pas pouvoir generer une lr si le debiteur n'a pas de for actif sur la periode
				ForDebiteurPrestationImposable forDebiteur= dpi.getDernierForDebiteur();
				if(forDebiteur!=null && DateRangeHelper.intersect(periodeSuivante,forDebiteur)){
					lrEditView.setDateDebutPeriode(periodeSuivante.getDateDebut());
					lrEditView.setDateFinPeriode(periodeSuivante.getDateFin());
					lrEditView.setPeriodicite(periodiciteSuivante.getPeriodiciteDecompte());
				}
				else {
					// il ne manque pas de LR, et la prochaine LR en suivant les périodicité n'est couverte par aucun
					// for fiscal ouvert...
					throw new ObjectNotFoundException("Toutes les LR du débiteur ont déjà été émises");
				}
			}
		}
		// Créée une premiere LR sur ce debiteur
		else {
			RegDate debutActivite = dpi.getDateDebutActivite();
			if (debutActivite == null) {
				throw new IllegalArgumentException("Le débiteur n°" + dpi.getNumero() +
						" ne possède pas de date de début d'activité. Absence de for fiscal  ?");
			}
			final PeriodiciteDecompte periodicite = dpi.getPeriodiciteAt(debutActivite).getPeriodiciteDecompte();
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
		if(lrEditView.getDateDebutPeriode()!=null){
			lrEditView.setSansSommation(dpi.getSansRappel());
			lrEditView.setImprimable(true);
			lrEditView.setDelaiAccorde(delaisService.getDateFinDelaiRetourListeRecapitulative(aujourdhui, lrEditView.getRegDateFinPeriode()));
			setDroitLR(lrEditView, dpi);
		}

		return lrEditView;
	}

	private DateRange getPeriodeSuivante(DebiteurPrestationImposable dpi, Periodicite periodiciteSuivante, DeclarationImpotSource lrPrecedente) {
		RegDate dateDebutPeriode = null;
		RegDate dateFinPeriode = null;
		RegDate dateFinLR = lrPrecedente.getDateFin();
		Periodicite periodicitePrecedente = dpi.getPeriodiciteAt(dateFinLR);
		if(periodicitePrecedente.getPeriodiciteDecompte()!=periodiciteSuivante.getPeriodiciteDecompte()){
			//Changement de periodicite,
			RegDate dateDebut = periodiciteSuivante.getDateDebut();
			if(periodiciteSuivante.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE){
				PeriodeDecompte periode = periodiciteSuivante.getPeriodeDecompte();
				final DateRange periodeUnique = periode.getPeriodeCourante(dateDebut);
				dateDebutPeriode = periodeUnique.getDateDebut();
				dateFinPeriode =  periodeUnique.getDateFin();
			}
			else{
				dateDebutPeriode = periodiciteSuivante.getDebutPeriode(dateDebut);
				dateFinPeriode =  periodiciteSuivante.getFinPeriode(dateDebutPeriode);

			}


		}else{

			if(periodiciteSuivante.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE){
				PeriodeDecompte periode = periodicitePrecedente.getPeriodeDecompte();
				final DateRange periodeUnique = periode.getPeriodeSuivante(dateFinLR);
				dateDebutPeriode = periodeUnique.getDateDebut();
				dateFinPeriode =  periodeUnique.getDateFin();
			}
			else{
				dateDebutPeriode = periodicitePrecedente.getDebutPeriodeSuivante(dateFinLR);
				dateFinPeriode =  periodicitePrecedente.getFinPeriode(dateDebutPeriode);

			}

		}

		return new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode); 
	}

	private Periodicite getPeriodiciteSuivante(DebiteurPrestationImposable dpi, DeclarationImpotSource lr) {
		final Periodicite periodiciteCourante = dpi.getPeriodiciteAt(lr.getDateDebut());
		DateRange rangeSuivant = null;
		final RegDate dateFinLR = lr.getDateFin();
		if (PeriodiciteDecompte.UNIQUE == periodiciteCourante.getPeriodiciteDecompte()) {
			PeriodeDecompte periodeDecompte = periodiciteCourante.getPeriodeDecompte();
			rangeSuivant = periodeDecompte.getPeriodeSuivante(dateFinLR);
		}
		else{
			RegDate debutPeriode = periodiciteCourante.getDebutPeriodeSuivante(dateFinLR);
			RegDate finPeriode = periodiciteCourante.getFinPeriode(debutPeriode);
			rangeSuivant = new DateRangeHelper.Range(debutPeriode, finPeriode);
		}
		Periodicite periodiciteSuivante = dpi.getPeriodiciteAt(rangeSuivant.getDateDebut());
		return periodiciteSuivante;
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
		PeriodeDecompte periodeCourante = dpi.getPeriodiciteAt(precDateFin).getPeriodeDecompte();
		DateRange rangeNextPeriode = periodeCourante.getPeriodeSuivante(precDateFin);
		lrEditView.setDateDebutPeriode(rangeNextPeriode.getDateDebut());
		lrEditView.setPeriodicite(periodicite);
		lrEditView.setDateFinPeriode(rangeNextPeriode.getDateFin());
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

		if (periodiciteDPI != periodiciteLR) {
			if (periodiciteDPI == PeriodiciteDecompte.TRIMESTRIEL
					&& periodiciteLR == PeriodiciteDecompte.MENSUEL
					&&		(month != 1) && (month != 4)
						&& 	(month != 7) && (month != 10) ) {
				return periodiciteLR;
			}
			if (periodiciteDPI == PeriodiciteDecompte.SEMESTRIEL
					&& 	(periodiciteLR == PeriodiciteDecompte.MENSUEL
							|| periodiciteLR == PeriodiciteDecompte.TRIMESTRIEL)
					&&		(month != 1) && (month != 7)  ) {
				return periodiciteLR;
			}
			if (periodiciteDPI == PeriodiciteDecompte.ANNUEL
					&& 	(periodiciteLR == PeriodiciteDecompte.MENSUEL
							|| periodiciteLR == PeriodiciteDecompte.TRIMESTRIEL
							|| periodiciteLR == PeriodiciteDecompte.SEMESTRIEL)
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
			throw new TiersNotFoundException(numero);
		}
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
		TiersGeneralView dpiView = tiersGeneralManager.getDebiteur(dpi, true);
		return dpiView;
	}

	/**
	 * Persiste en base et indexe le tiers modifie
	 *
	 * @param lrEditView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public DeclarationImpotSource save(ListeRecapDetailView lrEditView) {
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(lrEditView.getDpi().getNumero());

		EtatDeclaration etat = null;
		DeclarationImpotSource lr = null;
		if (lrEditView.getId() == null)	{
			lr = new DeclarationImpotSource();
			etat = new EtatDeclarationEmise();
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
			if (lrEditView.getRegDateRetour() != null && lrEditView.getRegDateRetour() != lr.getDateRetour()) {
				etat = new EtatDeclarationRetournee();
				etat.setDateObtention(RegDate.get(lrEditView.getDateRetour()));
				lr.addEtat(etat);
				evenementFiscalService.publierEvenementFiscalRetourLR(dpi, lr, RegDate.get(lrEditView.getDateRetour()));
			}
		}

		return lr;
	}

	/**
	 * Annule un delai
	 *
	 * @param lrEditView
	 */
	@Override
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
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void saveDelai(DelaiDeclarationView view) {
		DeclarationImpotSource lr = lrDAO.get(view.getIdDeclaration());
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(RegDate.get());
		delai.setAnnule(view.isAnnule());
		delai.setConfirmationEcrite(view.getConfirmationEcrite());
		delai.setDateDemande(view.getDateDemande());
		delai.setDelaiAccordeAu(view.getDelaiAccordeAu());
		lr.addDelai(delai);
	}

	/**
	 * Contrôle la présence de la LR
	 *
	 * @param id
	 */
	@Override
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
			DelaiDeclarationView delaiView = new DelaiDeclarationView(delai);
			delaiView.setFirst(lr.getPremierDelai() == delai.getDelaiAccordeAu());
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

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
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


	@Override
	public EditiqueResultat envoieImpressionLocalSommationLR(ListeRecapDetailView lrEditView) throws EditiqueException {
		//Sauvegarde de la DI
		final RegDate dateDuJour = RegDate.get();
		final DeclarationImpotSource lr = save(lrEditView);
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateDuJour,dateDuJour);
		lr.addEtat(etat);
		lrDAO.save(lr);

		try {
			//Envoi du flux xml à l'éditique
			return editiqueCompositionService.imprimeSommationLROnline(lr, dateDuJour);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@Override
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
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerLR (ListeRecapDetailView lrEditView) {
		DeclarationImpotSource lr = lrDAO.get(lrEditView.getId());
		lr.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationLR((DebiteurPrestationImposable) lr.getTiers(), lr, RegDate.get()) ;
	}

}
