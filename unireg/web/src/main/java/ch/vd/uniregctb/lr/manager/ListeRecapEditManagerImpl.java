package ch.vd.uniregctb.lr.manager;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
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
import ch.vd.uniregctb.declaration.view.DelaiDeclarationView;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
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
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
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

	protected static final Logger LOGGER = LoggerFactory.getLogger(ListeRecapEditManagerImpl.class);

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
	private ServiceInfrastructureService infraService;

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

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
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

		// [SIFISC-10283] LR annulable si EMISE, SOMMEE ou ECHUE
		lrEditView.setAnnulable(!lr.isAnnule() && etatLR.getEtat() != TypeEtatDeclaration.RETOURNEE);

		// [SIFISC-17743] ajout de délai seulement autorisée si lr seulement émise
		lrEditView.setAllowedDelai(lrEditView.isAllowedDelai() && etatLR.getEtat() == TypeEtatDeclaration.EMISE);

		return lrEditView;
	}


	private void setDroitLR(ListeRecapDetailView lrEditView, DebiteurPrestationImposable dpi) {
		if (SecurityHelper.isGranted(securityProvider, Role.LR)) {
			lrEditView.setAllowedDelai(true);
		}
		else {
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

		List<ListeRecapDetailView> lrsView = new ArrayList<>();
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
		delaiView.setDateTraitement(RegDate.get());

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
			final List<DateRange> lrTrouvees = new ArrayList<>();
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
				throw new IllegalArgumentException(String.format("Le débiteur n°%d ne possède pas de date de début d'activité. Absence de for fiscal  ?", dpi.getNumero()));
			}
			final Periodicite periodicite = dpi.getPeriodiciteAt(debutActivite);
			if (periodicite == null) {
				throw new ObjectNotFoundException("Aucune périodicité exploitable pour le débiteur à la date de début d'activité");
			}
			final PeriodiciteDecompte periodiciteDecompte = periodicite.getPeriodiciteDecompte();
			if (PeriodiciteDecompte.UNIQUE == periodiciteDecompte) {
				setLRViewPremiereLrAvecPeriodiciteUnique(lrEditView, dpi);
			}
			else {
				final RegDate nouvDateDebut = periodiciteDecompte.getDebutPeriode(dateDebutActivite);
				lrEditView.setDateDebutPeriode(nouvDateDebut);
				lrEditView.setPeriodicite(periodiciteDecompte);
				lrEditView.setDateFinPeriode(periodiciteDecompte.getFinPeriode(nouvDateDebut));
			}
		}
		if (lrEditView.getDateDebutPeriode() != null) {
			lrEditView.setImprimable(true);
			lrEditView.setDelaiAccorde(delaisService.getDateFinDelaiRetourListeRecapitulative(aujourdhui, lrEditView.getRegDateFinPeriode()));
			setDroitLR(lrEditView, dpi);
		}

		return lrEditView;
	}

	private DateRange getPeriodeSuivante(DebiteurPrestationImposable dpi, Periodicite periodiciteSuivante, DeclarationImpotSource lrPrecedente) {
		final RegDate dateDebutPeriode;
		final RegDate dateFinPeriode;
		final RegDate dateFinLRPrecedente = lrPrecedente.getDateFin();
		final Periodicite periodicitePrecedente = dpi.getPeriodiciteAt(dateFinLRPrecedente);
		final RegDate dateDebutPeriodeSuivante = periodiciteSuivante.getDateDebut();
		if (periodicitePrecedente.getPeriodiciteDecompte() != periodiciteSuivante.getPeriodiciteDecompte()) {
			//Changement de periodicite,

			if(periodiciteSuivante.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE){
				final PeriodeDecompte periode = periodiciteSuivante.getPeriodeDecompte();
				final DateRange periodeUnique = periode.getPeriodeCourante(dateDebutPeriodeSuivante);
				dateDebutPeriode = periodeUnique.getDateDebut();
				dateFinPeriode =  periodeUnique.getDateFin();
			}
			else{
				dateDebutPeriode = periodiciteSuivante.getDebutPeriode(dateDebutPeriodeSuivante);
				dateFinPeriode =  periodiciteSuivante.getFinPeriode(dateDebutPeriode);
			}
		}
		else {
			if (periodiciteSuivante.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE) {

				final PeriodeDecompte periodeDecomptePrecedente = periodicitePrecedente.getPeriodeDecompte();
				final PeriodeDecompte periodeDecompteSuivante = periodiciteSuivante.getPeriodeDecompte();
				//SIFISC-15772 Si la période de décompte est differente entre les deux périodicités, il faut prendre la nouvelle sinon on se retrouve à proposer l'ancienne
				//période de décompte
				final PeriodeDecompte periode = periodeDecomptePrecedente == periodeDecompteSuivante ? periodeDecomptePrecedente:periodeDecompteSuivante;
				//En cas de changement de periode de décompte il faut également changer la date de référence pour le calcul de période
				final RegDate dateReferencePourCalculPeriode = periodeDecomptePrecedente == periodeDecompteSuivante ? dateFinLRPrecedente:dateDebutPeriodeSuivante;
				final DateRange periodeUnique = periode.getPeriodeSuivante(dateReferencePourCalculPeriode);
				dateDebutPeriode = periodeUnique.getDateDebut();
				dateFinPeriode =  periodeUnique.getDateFin();
			}
			else {
				dateDebutPeriode = periodicitePrecedente.getDebutPeriodeSuivante(dateFinLRPrecedente);
				dateFinPeriode =  periodicitePrecedente.getFinPeriode(dateDebutPeriode);
			}
		}

		return new DateRangeHelper.Range(dateDebutPeriode, dateFinPeriode);
	}

	private Periodicite getPeriodiciteSuivante(DebiteurPrestationImposable dpi, DeclarationImpotSource lr) {
		final Periodicite periodiciteCourante = dpi.getPeriodiciteAt(lr.getDateDebut());
		final DateRange rangeSuivant;
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
		return dpi.getPeriodiciteAt(rangeSuivant.getDateDebut());
	}

	private void setLRViewPremiereLrAvecPeriodiciteUnique(ListeRecapDetailView lrEditView, DebiteurPrestationImposable dpi) {
		final RegDate dateDebutActivite = dpi.getDateDebutActivite();
		final PeriodeDecompte periode = dpi.getPeriodiciteAt(dateDebutActivite).getPeriodeDecompte();
		final DateRange rangePeriode;
		final DateRange rangePeriodeCourante = periode.getPeriodeCourante(dateDebutActivite);
		if (rangePeriodeCourante.getDateFin().isBefore(dateDebutActivite)) {
			rangePeriode = periode.getPeriodeSuivante(dateDebutActivite);
		}
		else {
			rangePeriode = rangePeriodeCourante;
		}

		lrEditView.setDateDebutPeriode(rangePeriode.getDateDebut());
		lrEditView.setDateFinPeriode(rangePeriode.getDateFin());
		lrEditView.setPeriodicite(PeriodiciteDecompte.UNIQUE);
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
			lr.setDateDebut(RegDateHelper.get(lrEditView.getDateDebutPeriode()));
			lr.setDateFin(RegDateHelper.get(lrEditView.getDateFinPeriode()));
			lr.setModeCommunication(dpi.getModeCommunication());
			lr.setPeriodicite(lrEditView.getPeriodicite());
			int annee = RegDateHelper.get(lrEditView.getDateDebutPeriode()).year();
			PeriodeFiscale periodeFiscale = periodeFiscaleDAO.getPeriodeFiscaleByYear(annee);
			if (periodeFiscale == null) {
				throw new ValidationException(lr, "Veuillez initialiser la période fiscale correspondante.");
			}
			lr.setPeriode(periodeFiscale);

			//DelaiDeclaration delai = new DelaiDeclaration();
			//delai.setDateDemande(theDateDemande);
			//delai.setDateTraitement(RegDate.get());
			//TODO (FNR) décaler date du délai si samedi, dimanche ou jours férié
			//delai.setDelaiAccordeAu(lr.getDateFin().addMonths(1));

			DelaiDeclaration delai = new DelaiDeclaration();
			delai.setEtat(EtatDelaiDeclaration.ACCORDE);
			delai.setDelaiAccordeAu(lrEditView.getRegDelaiAccorde());
			delai.setDateDemande(RegDate.get());
			delai.setDateTraitement(RegDate.get());
			lr.addDelai(delai);
			lr.setTiers(dpi);
			lr = lrDAO.save(lr);
			dpi.addDeclaration(lr);
			//tiersDAO.save(dpi);
			evenementFiscalService.publierEvenementFiscalEmissionListeRecapitulative(lr, RegDate.get());
		}
		else {
			lr = lrDAO.get(lrEditView.getId());
			if (lrEditView.getRegDateRetour() != null && lrEditView.getRegDateRetour() != lr.getDateRetour()) {
				etat = new EtatDeclarationRetournee();
				etat.setDateObtention(RegDateHelper.get(lrEditView.getDateRetour()));
				lr.addEtat(etat);
				evenementFiscalService.publierEvenementFiscalQuittancementListeRecapitulative(lr, RegDateHelper.get(lrEditView.getDateRetour()));
			}
		}

		return lr;
	}

	/**
	 * Persiste en base et indexe le tiers modifie
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void saveDelai(DelaiDeclarationView view) {
		final DeclarationImpotSource lr = lrDAO.get(view.getIdDeclaration());
		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDeclaration.ACCORDE);
		delai.setDateTraitement(RegDate.get());
		delai.setAnnule(view.isAnnule());
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
		List<DelaiDeclarationView> delaisView = new ArrayList<>();
		for (DelaiDeclaration delai : lr.getDelais()) {
			DelaiDeclarationView delaiView = new DelaiDeclarationView(delai, infraService, getMessageSource());
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
			return editiqueCompositionService.imprimeLROnline(lr, TypeDocument.LISTE_RECAPITULATIVE);
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
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateDuJour, dateDuJour, null);
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

			Audit.info(String.format("Impression (%s/%s) d'un duplicata de LR pour le débiteur %d et la période [%s ; %s]",
			                         AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOIDSigle(),
			                         lr.getTiers().getNumero(),
			                         RegDateHelper.dateToDashString(lr.getDateDebut()),
			                         RegDateHelper.dateToDashString(lr.getDateFin())));
			
			return editiqueCompositionService.imprimeLROnline(lr, TypeDocument.LISTE_RECAPITULATIVE);
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
		final DeclarationImpotSource lr = lrDAO.get(lrEditView.getId());
		final EtatDeclaration etat = lr.getDernierEtat();
		if (etat == null || etat.getEtat() != TypeEtatDeclaration.RETOURNEE) {
			lr.setAnnule(true);
			evenementFiscalService.publierEvenementFiscalAnnulationListeRecapitulative(lr); ;
		}
		else {
			throw new ActionException("La liste récapitulative est quittancée. Son annulation est donc impossible.");
		}
	}

}
