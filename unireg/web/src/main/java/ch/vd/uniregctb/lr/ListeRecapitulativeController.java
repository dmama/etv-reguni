package ch.vd.uniregctb.lr;

import javax.jms.JMSException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.DebiteurPrestationImposableNotFoundException;
import ch.vd.uniregctb.common.EditiqueCommunicationException;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.common.TicketTimeoutException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.declaration.DeclarationGenerationOperation;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.ListeRecapitulativeCriteria;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.lr.manager.ListeRecapListManager;
import ch.vd.uniregctb.lr.validator.DelaiAddViewValidator;
import ch.vd.uniregctb.lr.validator.ListeRecapitulativeAddViewValidator;
import ch.vd.uniregctb.lr.view.DelaiAddView;
import ch.vd.uniregctb.lr.view.ListeRecapitulativeAddView;
import ch.vd.uniregctb.lr.view.ListeRecapitulativeDetailView;
import ch.vd.uniregctb.lr.view.ListeRecapitulativeSearchResult;
import ch.vd.uniregctb.lr.view.ListesRecapitulativesView;
import ch.vd.uniregctb.lr.view.SearchResultsHidingCause;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.PeriodeFiscaleService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.utils.WebContextUtils;

@Controller
@RequestMapping("/lr")
public class ListeRecapitulativeController {

	private static final String ACCESS_DENIED = "Vous ne possédez aucun droit d'accès aux fonctionalités d'édition des listes récapitulatives";

	private static final String NOM_COMMANDE_RECHERCHE_LR = "critereRechercheListesRecapitulatives";
	private static final String NOM_PARAM_EFFACER = "effacer";
	private static final String NOM_SESSION_CRITERES_RECHERCHE_LR = "critereRechercheListesRecapitulativesInSession";

	private static final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> ERREUR_DUPLICATA_EDITIQUE =
			resultat -> {
				final String message = String.format("%s Veuillez ré-essayer plus tard.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
				throw new EditiqueCommunicationException(message);
			};

	private static final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> ERREUR_ORIGINAL_EDITIQUE =
			resultat -> {
				final String message = String.format("%s Veuillez imprimer un duplicata de la liste récapitulative.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
				throw new EditiqueCommunicationException(message);
			};

	private ListeRecapListManager lrListManager;
	private TiersMapHelper tiersMapHelper;
	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService infraService;
	private MessageSource messageSource;
	private RetourEditiqueControllerHelper retourEditiqueHelper;
	private EditiqueCompositionService editiqueCompositionService;
	private ListeRecapService lrService;
	private DelaisService delaisService;
	private PeriodeFiscaleService periodeFiscaleService;
	private EvenementFiscalService evenementFiscalService;
	private TicketService ticketService;

	public void setLrListManager(ListeRecapListManager lrListManager) {
		this.lrListManager = lrListManager;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setRetourEditiqueHelper(RetourEditiqueControllerHelper retourEditiqueHelper) {
		this.retourEditiqueHelper = retourEditiqueHelper;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setLrService(ListeRecapService lrService) {
		this.lrService = lrService;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setPeriodeFiscaleService(PeriodeFiscaleService periodeFiscaleService) {
		this.periodeFiscaleService = periodeFiscaleService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setTicketService(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	//
	// Méthodes utilitaires
	//

	@NotNull
	private DeclarationImpotSource getListeRecapitulative(long id) {
		final DeclarationImpotSource lr = hibernateTemplate.get(DeclarationImpotSource.class, id);
		if (lr == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.lr.inexistante" , null, WebContextUtils.getDefaultLocale()));
		}
		return lr;
	}

	@NotNull
	private DebiteurPrestationImposable getDebiteurPrestationImposable(long id) {
		final DebiteurPrestationImposable dpi = hibernateTemplate.get(DebiteurPrestationImposable.class, id);
		if (dpi == null) {
			throw new DebiteurPrestationImposableNotFoundException(id);
		}
		return dpi;
	}


	//
	// Ecran de recherche des LR
	//

	@InitBinder(value = NOM_COMMANDE_RECHERCHE_LR)
	public void initCritereRechercheListesRecapitulativesBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * @param model le modèle
	 * @param request la requête HTTP
	 * @param effacer [optionel] booléen qui indique s'il faut effacer tous les critères de recherche
	 * @param realSearch [optionel] booléen qui que le formulaire a réellement été soumis (par rapport à un simple retour sur la page)
	 * @param criteriaView critères de recherche
	 * @param bindingResult résultats de binding par rapport aux critères de recherche
	 * @return la vue
	 * @throws AdresseException en cas de souci avec la résolution de l'adresse
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String searchListeRecapitulative(Model model,
	                                        HttpServletRequest request,
	                                        @RequestParam(value = NOM_PARAM_EFFACER, required = false, defaultValue = "false") boolean effacer,
	                                        @RequestParam(value = "realSearch", required = false, defaultValue = "false") boolean realSearch,
	                                        @ModelAttribute(value = NOM_COMMANDE_RECHERCHE_LR) @Valid ListeRecapitulativeCriteria criteriaView,
	                                        BindingResult bindingResult) throws AdresseException {

		if (bindingResult.hasErrors()) {
			return showSearchListeRecapitulative(model, criteriaView, Collections.emptyList(), 0, SearchResultsHidingCause.INVALID_CRITERIA);
		}
		if (effacer) {
			request.getSession().removeAttribute(NOM_SESSION_CRITERES_RECHERCHE_LR);
			return showSearchListeRecapitulative(model, new ListeRecapitulativeCriteria(), Collections.emptyList(), 0, SearchResultsHidingCause.EMPTY_CRITERIA);
		}

		final ListeRecapitulativeCriteria criteria = findApplicableCriteria(criteriaView, request.getSession(), realSearch);
		final List<ListeRecapitulativeSearchResult> resultats;
		final int totalSize;
		final boolean empty = criteria.isEmpty();
		if (empty) {
			resultats = Collections.emptyList();
			totalSize = 0;
		}
		else {
			final WebParamPagination pagination = new WebParamPagination(request, "lr", 25);
			resultats = lrListManager.find(criteria, pagination);
			totalSize = lrListManager.count(criteria);
		}

		return showSearchListeRecapitulative(model, criteria, resultats, totalSize, empty ? SearchResultsHidingCause.EMPTY_CRITERIA : null);
	}

	/**
	 * Reconstruit une structure de critères pour la recherche de listes récapitulatives d'après la vue donnée, et si celle-ci est vide,
	 * prend en compte le contenu de la session
	 * @param view source principale des données pour les critères de recherche
	 * @param session source secondaire (si la première fournit des critères vides) pour les critères de recherche
	 * @param realSearch <code>true</code> si le formulaire de recherche a été réellement (re-)soumis, i.e. si la donnée présente en session doit de toute façon être ignorée...
	 * @return les critères de recherche à prendre en compte
	 */
	private static ListeRecapitulativeCriteria findApplicableCriteria(ListeRecapitulativeCriteria view, HttpSession session, boolean realSearch) {
		if (!view.isEmpty() || realSearch) {
			session.setAttribute(NOM_SESSION_CRITERES_RECHERCHE_LR, view);
			return view;
		}

		final ListeRecapitulativeCriteria fromSession = (ListeRecapitulativeCriteria) session.getAttribute(NOM_SESSION_CRITERES_RECHERCHE_LR);
		if (fromSession == null) {
			final ListeRecapitulativeCriteria newCriteria = new ListeRecapitulativeCriteria();
			session.setAttribute(NOM_SESSION_CRITERES_RECHERCHE_LR, newCriteria);
			return newCriteria;
		}
		return fromSession;
	}

	private String showSearchListeRecapitulative(Model model,
	                                             ListeRecapitulativeCriteria criteriaView,
	                                             List<ListeRecapitulativeSearchResult> resultats,
	                                             int totalSize,
	                                             @Nullable SearchResultsHidingCause hidingCause) {

		// add values for the combos
		model.addAttribute("categoriesImpotSource", tiersMapHelper.getMapCategorieImpotSource());
		model.addAttribute("periodicitesDecompte", tiersMapHelper.getMapPeriodiciteDecompte());
		model.addAttribute("etatsDocument", tiersMapHelper.getMapTypeEtatListeRecapitulative());
		model.addAttribute("modesCommunication", tiersMapHelper.getMapModeCommunication());

		// the criteria
		model.addAttribute(NOM_COMMANDE_RECHERCHE_LR, criteriaView);
		model.addAttribute("resultsHidingCause", hidingCause);

		// the search results
		model.addAttribute("lrs", resultats);
		model.addAttribute("resultSize", totalSize);

		return "lr/recherche/list";
	}

	//
	// Ecrans d'édition des LR sur un débiteur IS
	//

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@SecurityCheck(rolesToCheck = Role.LR, accessDeniedMessage = ACCESS_DENIED)
	@RequestMapping(value = "/edit-debiteur.do", method = RequestMethod.GET)
	public String editListesRecapitulatives(Model model, @RequestParam(value = "numero") long idDebiteur) {
		final DebiteurPrestationImposable dpi = getDebiteurPrestationImposable(idDebiteur);
		final ListesRecapitulativesView view = new ListesRecapitulativesView(dpi);
		model.addAttribute("idDebiteur", idDebiteur);
		model.addAttribute("listesRecapitulatives", view.getLrs());
		return "lr/edit/edit-debiteur";
	}

	@InitBinder(value = "addListeCommand")
	public void initAddListeCommandBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(new ListeRecapitulativeAddViewValidator());
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@SecurityCheck(rolesToCheck = Role.LR, accessDeniedMessage = ACCESS_DENIED)
	@RequestMapping(value = "/add-lr.do", method = RequestMethod.GET)
	public String addListe(Model model, @RequestParam(value = "idDebiteur") long idDebiteur) {

		// il faut valider que le débiteur a des fors fiscaux, non ?
		final DebiteurPrestationImposable dpi = getDebiteurPrestationImposable(idDebiteur);
		final List<ForFiscal> forsFiscauxNonAnnules = dpi.getForsFiscauxNonAnnules(false);
		if (forsFiscauxNonAnnules == null || forsFiscauxNonAnnules.isEmpty()) {
			//la dpi ne possède pas de for => création LR interdite
			throw new ActionException("Impossible de créer une liste récapitulative pour un débiteur de prestation imposable sans for");
		}

		final DateRange range = findNextAvailableLRSlot(dpi);
		final RegDate delaiAccorde = delaisService.getDateFinDelaiRetourListeRecapitulative(RegDate.get(), range.getDateFin());
		final ListeRecapitulativeAddView view = new ListeRecapitulativeAddView(dpi, range, delaiAccorde);
		return showAddListe(model, view);
	}

	private String showAddListe(Model model, ListeRecapitulativeAddView view) {
		model.addAttribute("addListeCommand", view);
		return "lr/edit/add-lr";
	}

	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.LR, accessDeniedMessage = ACCESS_DENIED)
	@RequestMapping(value = "/add-lr.do", method = RequestMethod.POST)
	public String doAddList(Model model, @Valid @ModelAttribute(value = "addListeCommand") ListeRecapitulativeAddView view, BindingResult bindingResult) throws EditiqueException, JMSException, IOException, InterruptedException {
		final DebiteurPrestationImposable dpi = getDebiteurPrestationImposable(view.getIdDebiteur());
		if (bindingResult.hasErrors()) {
			return showAddListe(model, view);
		}

		final DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.addEtat(new EtatDeclarationEmise(RegDate.get()));
		lr.setDateDebut(view.getDateDebut());
		lr.setDateFin(view.getDateFin());
		lr.setModeCommunication(dpi.getModeCommunication());

		final List<Periodicite> periodicitesNonAnnulees = dpi.getPeriodicitesNonAnnulees(true);
		final PeriodiciteDecompte periodicite = Optional.of(view.getDateDebut())
				.map(date -> DateRangeHelper.rangeAt(periodicitesNonAnnulees, date))
				.map(Periodicite::getPeriodiciteDecompte)
				.orElse(null);
		lr.setPeriodicite(periodicite);

		final PeriodeFiscale pf = periodeFiscaleService.get(view.getDateFin().year());
		if (pf == null) {
			throw new ValidationException(lr, "Veuillez initialiser la période fiscale correspondante");
		}
		lr.setPeriode(pf);

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateDemande(RegDate.get());
		delai.setDateTraitement(RegDate.get());
		delai.setDelaiAccordeAu(view.getDelaiAccorde());
		delai.setEtat(EtatDelaiDeclaration.ACCORDE);
		lr.addDelai(delai);

		lr.setTiers(dpi);
		final DeclarationImpotSource saved = hibernateTemplate.merge(lr);
		dpi.addDeclaration(saved);
		evenementFiscalService.publierEvenementFiscalEmissionListeRecapitulative(saved, RegDate.get());

		// et finalement l'envoi à l'éditique...
		final DeclarationGenerationOperation tickettingKey = new DeclarationGenerationOperation(dpi.getNumero());
		try {
			final TicketService.Ticket ticket = ticketService.getTicket(tickettingKey, Duration.ofMillis(500));
			try {
				final EditiqueResultat resultat = editiqueCompositionService.imprimeLROnline(saved, TypeDocument.LISTE_RECAPITULATIVE);
				return retourEditiqueHelper.traiteRetourEditiqueAfterRedirect(resultat, "lr", "redirect:edit-debiteur.do?numero=" + dpi.getNumero(), null, null, ERREUR_ORIGINAL_EDITIQUE);
			}
			finally {
				ticket.release();
			}
		}
		catch (TicketTimeoutException e) {
			throw new ActionException("Une LR est actuellement en cours d'impression pour ce débiteur. Veuillez ré-essayer ultérieurement.", e);
		}
	}

	private DateRange findNextAvailableLRSlot(DebiteurPrestationImposable dpi) {
		// y a-t-il des LR, déjà ?
		final DeclarationImpotSource last = dpi.getDerniereDeclaration(DeclarationImpotSource.class);
		if (last != null) {
			final List<DateRange> lrTrouvees = new ArrayList<>();
			final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, RegDate.get(), lrTrouvees);
			final DateRange periodeInteressante = new DateRangeHelper.Range(null, RegDate.get());
			//[UNIREG-3120] la LR à ajouter doit recouper au moins une période d'activité du débiteur
			if (lrManquantes != null && !lrManquantes.isEmpty()
					&& (periodeInteressante.isValidAt(lrManquantes.get(0).getDateFin()) || periodeInteressante.isValidAt(lrManquantes.get(0).getDateDebut()))
					&& !DateRangeHelper.intersect(lrManquantes.get(0), lrTrouvees)) {
				return lrManquantes.get(0);
			}
			else {
				final Periodicite periodiciteSuivante = getPeriodiciteSuivante(dpi, last);
				if (periodiciteSuivante == null) {
					throw new IllegalArgumentException("Incohérence des données : le débiteur n°" + dpi.getNumero() +
							                                   " ne possède pas de périodicité à la date [" + RegDateHelper.dateToDisplayString(last.getDateFin()) +
							                                   "] alors même qu'il existe un for fiscal. Problème de création des périodicités ?");
				}

				final DateRange periodeSuivante = getPeriodeSuivante(dpi, periodiciteSuivante, last);
				//UNIREG-3120 - 2 On ne doit pas pouvoir generer une lr si le debiteur n'a pas de for actif sur la periode
				final ForDebiteurPrestationImposable forDebiteur = dpi.getDernierForDebiteur();
				if (forDebiteur != null && DateRangeHelper.intersect(periodeSuivante, forDebiteur)) {
					return periodeSuivante;
				}
				else {
					// il ne manque pas de LR, et la prochaine LR en suivant les périodicité n'est couverte par aucun
					// for fiscal ouvert...
					throw new ObjectNotFoundException("Toutes les LR du débiteur ont déjà été émises");
				}
			}
		}
		else {
			final RegDate debutActivite = dpi.getDateDebutActivite();
			if (debutActivite == null) {
				throw new IllegalArgumentException(String.format("Le débiteur n°%d ne possède pas de date de début d'activité. Absence de for fiscal  ?", dpi.getNumero()));
			}
			final Periodicite periodicite = dpi.getPeriodiciteAt(debutActivite);
			if (periodicite == null) {
				throw new ObjectNotFoundException("Aucune périodicité exploitable pour le débiteur à la date de début d'activité");
			}
			final PeriodiciteDecompte periodiciteDecompte = periodicite.getPeriodiciteDecompte();
			if (PeriodiciteDecompte.UNIQUE == periodiciteDecompte) {
				final PeriodeDecompte periode = dpi.getPeriodiciteAt(debutActivite).getPeriodeDecompte();
				final DateRange rangePeriodeCourante = periode.getPeriodeCourante(debutActivite);
				if (rangePeriodeCourante.getDateFin().isBefore(debutActivite)) {
					return periode.getPeriodeSuivante(debutActivite);
				}
				else {
					return rangePeriodeCourante;
				}
			}
			else {
				final RegDate nouvDateDebut = periodiciteDecompte.getDebutPeriode(debutActivite);
				final RegDate nouvDateFin = periodiciteDecompte.getFinPeriode(debutActivite);
				return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);
			}
		}
	}

	private static DateRange getPeriodeSuivante(DebiteurPrestationImposable dpi, Periodicite periodiciteSuivante, DeclarationImpotSource lrPrecedente) {
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

	private static Periodicite getPeriodiciteSuivante(DebiteurPrestationImposable dpi, DeclarationImpotSource lr) {
		final Periodicite periodiciteCourante = dpi.getPeriodiciteAt(lr.getDateDebut());
		final DateRange rangeSuivant;
		final RegDate dateFinLR = lr.getDateFin();
		if (PeriodiciteDecompte.UNIQUE == periodiciteCourante.getPeriodiciteDecompte()) {
			final PeriodeDecompte periodeDecompte = periodiciteCourante.getPeriodeDecompte();
			rangeSuivant = periodeDecompte.getPeriodeSuivante(dateFinLR);
		}
		else{
			final RegDate debutPeriode = periodiciteCourante.getDebutPeriodeSuivante(dateFinLR);
			final RegDate finPeriode = periodiciteCourante.getFinPeriode(debutPeriode);
			rangeSuivant = new DateRangeHelper.Range(debutPeriode, finPeriode);
		}
		return dpi.getPeriodiciteAt(rangeSuivant.getDateDebut());
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@SecurityCheck(rolesToCheck = Role.LR, accessDeniedMessage = ACCESS_DENIED)
	@RequestMapping(value = "/edit-lr.do", method = RequestMethod.GET)
	public String editListe(Model model, @RequestParam(value = "id") long idListe) {
		final DeclarationImpotSource lr = getListeRecapitulative(idListe);
		final ListeRecapitulativeDetailView view = new ListeRecapitulativeDetailView(lr, infraService, messageSource);
		model.addAttribute("lr", view);
		return "lr/edit/edit-lr";
	}

	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.LR, accessDeniedMessage = ACCESS_DENIED)
	@RequestMapping(value = "/duplicata.do", method = RequestMethod.POST)
	public String duplicata(@RequestParam(value = "idListe") long idListe) throws IOException, JMSException, EditiqueException {

		final DeclarationImpotSource lr = getListeRecapitulative(idListe);

		Audit.info(String.format("Impression (%s/%s) d'un duplicata de LR pour le débiteur %d et la période [%s ; %s]",
		                         AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOIDSigle(),
		                         lr.getTiers().getNumero(),
		                         RegDateHelper.dateToDashString(lr.getDateDebut()),
		                         RegDateHelper.dateToDashString(lr.getDateFin())));

		final EditiqueResultat resultat = editiqueCompositionService.imprimeLROnline(lr, TypeDocument.LISTE_RECAPITULATIVE);
		return retourEditiqueHelper.traiteRetourEditiqueAfterRedirect(resultat, "lr", "redirect:edit-lr.do?id=" + idListe, null, null, ERREUR_DUPLICATA_EDITIQUE);
	}

	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.LR, accessDeniedMessage = ACCESS_DENIED)
	@RequestMapping(value = "/annuler.do", method = RequestMethod.POST)
	public String annulerListeRecapitulative(@RequestParam(value = "idListe") long idListe) {
		final DeclarationImpotSource lr = getListeRecapitulative(idListe);
		if (lr.isAnnule()) {
			throw new ActionException("Cette liste récapitulative est déjà annulée.");
		}
		lr.setAnnule(true);
		return "redirect:edit-debiteur.do?numero=" + lr.getTiers().getNumero();
	}

	@InitBinder(value = "addDelaiCommand")
	public void initAddDelaiCommandBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(new DelaiAddViewValidator());
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@SecurityCheck(rolesToCheck = Role.LR, accessDeniedMessage = ACCESS_DENIED)
	@RequestMapping(value = "/add-delai.do", method = RequestMethod.GET)
	public String addDelai(Model model, @RequestParam(value = "idListe") long idListe) {
		final DeclarationImpotSource lr = getListeRecapitulative(idListe);
		return showAddDelai(model, lr, new DelaiAddView(lr));
	}

	private String showAddDelai(Model model, DelaiAddView view) {
		final DeclarationImpotSource lr = getListeRecapitulative(view.getIdListe());
		return showAddDelai(model, lr, view);
	}

	private String showAddDelai(Model model, DeclarationImpotSource lr, DelaiAddView view) {
		model.addAttribute("idDebiteur", lr.getTiers().getNumero());
		model.addAttribute("pf", lr.getPeriode().getAnnee());
		model.addAttribute("dateDebut", lr.getDateDebut());
		model.addAttribute("dateFin", lr.getDateFin());
		model.addAttribute("idListe", lr.getId());
		model.addAttribute("ancienDelai", lr.getDernierDelaiAccorde().getDelaiAccordeAu());
		model.addAttribute("addDelaiCommand", view);
		return "lr/edit/add-delai";
	}

	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = Role.LR, accessDeniedMessage = ACCESS_DENIED)
	@RequestMapping(value = "/add-delai.do", method = RequestMethod.POST)
	public String doAddDelai(Model model, @Valid @ModelAttribute(value = "addDelaiCommand") DelaiAddView view, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showAddDelai(model, view);
		}

		final DeclarationImpotSource lr = getListeRecapitulative(view.getIdListe());
		final DelaiDeclaration ancienDelai = lr.getDernierDelaiAccorde();
		if (RegDateHelper.isBeforeOrEqual(view.getDelaiAccorde(), ancienDelai.getDelaiAccordeAu(), NullDateBehavior.EARLIEST)) {
			bindingResult.rejectValue("delaiAccorde", "error.delai.accorde.invalide");
			return showAddDelai(model, lr, view);
		}

		final DelaiDeclaration nouveauDelai = new DelaiDeclaration();
		nouveauDelai.setDateDemande(view.getDateDemande());
		nouveauDelai.setDateTraitement(RegDate.get());
		nouveauDelai.setDelaiAccordeAu(view.getDelaiAccorde());
		nouveauDelai.setEtat(EtatDelaiDeclaration.ACCORDE);
		lr.addDelai(nouveauDelai);

		return "redirect:edit-lr.do?id=" + lr.getId();
	}
}
