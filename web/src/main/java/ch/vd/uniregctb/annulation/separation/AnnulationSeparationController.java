package ch.vd.uniregctb.annulation.separation;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.validator.TiersCriteriaValidator;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.utils.WebContextUtils;

@Controller
@RequestMapping("/annulation/separation")
public class AnnulationSeparationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnulationSeparationController.class);

	private static final String CRITERIA_SESSION_NAME = "AnnulationSeparationCriteria";

	private ControllerUtils controllerUtils;
	private TiersService tiersService;
	private MetierService metierService;
	private MessageSource messageSource;
	private TiersMapHelper tiersMapHelper;

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	private static TiersCriteriaView getCriteriaFromSessionOrNew(HttpSession session) {
		return Optional.of(session)
				.map(s -> s.getAttribute(CRITERIA_SESSION_NAME))
				.map(TiersCriteriaView.class::cast)
				.orElseGet(TiersCriteriaView::new);
	}

	private static void fillMandatorySearchCriteria(TiersCriteriaView view) {
		view.setTypeTiersImperatif(TiersCriteria.TypeTiers.MENAGE_COMMUN);
	}

	private List<TiersIndexedDataView> searchTiers(TiersCriteriaView criteria) {
		return tiersService.search(criteria.asCore()).stream()
				.map(TiersIndexedDataView::new)
				.collect(Collectors.toList());
	}

	@InitBinder(value = "searchCommand")
	public void initSearchCommandBinder(WebDataBinder binder) {
		binder.setValidator(new TiersCriteriaValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = "date")
	public void initCommandBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String resetSearchCriteria(HttpSession session) {
		session.removeAttribute(CRITERIA_SESSION_NAME);
		return "redirect:list.do";
	}

	@Transactional(readOnly = true)
	@RequestMapping(value = "list.do", method = RequestMethod.GET)
	public String showList(Model model, HttpSession session) {
		final TiersCriteriaView view = getCriteriaFromSessionOrNew(session);
		fillMandatorySearchCriteria(view);

		String searchError = null;
		try {
			final List<TiersIndexedDataView> results = searchTiers(view).stream()
					.filter(found -> isDernierForFiscalPrincipalFermePourSeparation(found.getNumero()))
					.collect(Collectors.toList());
			model.addAttribute("list", results);
		}
		catch (EmptySearchCriteriaException e) {
			// rien à faire, aucun résultat, c'est tout...
		}
		catch (TooManyResultsIndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			if (e.getNbResults() > 0) {
				searchError = messageSource.getMessage("error.preciser.recherche.trouves", new Object[] {String.valueOf(e.getNbResults())}, WebContextUtils.getDefaultLocale());
			}
			else {
				searchError = messageSource.getMessage("error.preciser.recherche", null, WebContextUtils.getDefaultLocale());
			}
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			searchError = messageSource.getMessage("error.recherche", null, WebContextUtils.getDefaultLocale());
		}
		model.addAttribute("searchError", searchError);

		return showList(model, view);
	}

	private boolean isDernierForFiscalPrincipalFermePourSeparation(long idTiers) {
		final Tiers tiers = tiersService.getTiers(idTiers);
		return tiers != null
				&& tiers instanceof ContribuableImpositionPersonnesPhysiques
				&& tiersService.isDernierForFiscalPrincipalFermePourSeparation((ContribuableImpositionPersonnesPhysiques) tiers);
	}

	private String showList(Model model, TiersCriteriaView criteria) {
		model.addAttribute("typesRechercheNom", tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute("searchCommand", criteria);
		return "annulation/separation/list";
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	public String doSearch(Model model,
	                       HttpSession session,
	                       @Valid @ModelAttribute(value = "searchCommand") TiersCriteriaView criteria,
	                       BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return showList(model, criteria);
		}

		session.setAttribute(CRITERIA_SESSION_NAME, criteria);
		return "redirect:list.do";
	}

	@NotNull
	private MenageCommun getMenage(long idMenage) {
		final Tiers tiersMenage = tiersService.getTiers(idMenage);
		if (tiersMenage == null || !(tiersMenage instanceof MenageCommun)) {
			throw new TiersNotFoundException(idMenage);
		}
		return (MenageCommun) tiersMenage;
	}

	@Transactional(readOnly = true)
	@RequestMapping(value = "/recap.do", method = RequestMethod.GET)
	public String showRecapitulatif(Model model, @RequestParam(value = "numeroCple") long idMenage) {
		controllerUtils.checkAccesDossierEnLecture(idMenage);

		final MenageCommun menage = getMenage(idMenage);

		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menage, null);
		final Long idPrincipal = Optional.of(couple).map(EnsembleTiersCouple::getPrincipal).map(Tiers::getNumero).orElse(null);
		final Long idConjoint = Optional.of(couple).map(EnsembleTiersCouple::getConjoint).map(Tiers::getNumero).orElse(null);

		model.addAttribute("idMenage", idMenage);
		model.addAttribute("idContribuablePrincipal", idPrincipal);
		model.addAttribute("idContribuableConjoint", idConjoint);

		final ForFiscalPrincipal dernierForPrincipal = menage.getDernierForFiscalPrincipal();
		if (dernierForPrincipal != null && dernierForPrincipal.getDateFin() != null && dernierForPrincipal.getMotifFermeture() == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT) {
			model.addAttribute("dateSeparation", dernierForPrincipal.getDateFin().getOneDayAfter());
		}

		return "annulation/separation/recap";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/commit.do", method = RequestMethod.POST)
	public String doAnnulerMenage(@RequestParam(value = "numeroCple") long idMenage, @RequestParam(value = "date") RegDate dateSeparation) {
		controllerUtils.checkAccesDossierEnEcriture(idMenage);
		controllerUtils.checkTraitementContribuableAvecDecisionAci(idMenage);

		final MenageCommun menageCommun = getMenage(idMenage);
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menageCommun, dateSeparation.getOneDayBefore());
		if (couple.getPrincipal() != null) {
			final long id = couple.getPrincipal().getNumero();
			controllerUtils.checkAccesDossierEnEcriture(id);
			controllerUtils.checkTraitementContribuableAvecDecisionAci(id);
		}
		if (couple.getConjoint() != null) {
			final long id = couple.getConjoint().getNumero();
			controllerUtils.checkAccesDossierEnEcriture(id);
			controllerUtils.checkTraitementContribuableAvecDecisionAci(id);
		}
		
		try {
			metierService.annuleSeparation(menageCommun, dateSeparation, null);
		}
		catch (MetierServiceException e) {
			final StringBuilder b = new StringBuilder();
			b.append("Exception lors de l'annulation de la séparation du ménage composé ");
			if (couple.getPrincipal() != null && couple.getConjoint() != null) {
				b.append("des tiers ").append(FormatNumeroHelper.numeroCTBToDisplay(couple.getPrincipal().getNumero()));
				b.append(" et ").append(FormatNumeroHelper.numeroCTBToDisplay(couple.getConjoint().getNumero()));
			}
			else if (couple.getPrincipal() != null) {
				b.append("du tiers ").append(FormatNumeroHelper.numeroCTBToDisplay(couple.getPrincipal().getNumero()));
			}
			else {
				b.append("de tiers inconnus");
			}
			LOGGER.error(b.toString(), e);
			throw new ActionException(e.getMessage());
		}
		return "redirect:/tiers/visu.do?id=" + idMenage;
	}

}
