package ch.vd.unireg.identification.contribuable;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.evenement.identification.contribuable.Demande;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.unireg.evenement.identification.contribuable.TypeDemande;
import ch.vd.unireg.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.unireg.identification.contribuable.manager.IdentificationMessagesListManager;
import ch.vd.unireg.identification.contribuable.manager.IdentificationMessagesStatsManager;
import ch.vd.unireg.identification.contribuable.validator.IdentificationManuelleMessageEditValidator;
import ch.vd.unireg.identification.contribuable.validator.IdentificationMessageValidator;
import ch.vd.unireg.identification.contribuable.validator.NonIdentificationMessageEditValidator;
import ch.vd.unireg.identification.contribuable.view.DemandeIdentificationView;
import ch.vd.unireg.identification.contribuable.view.IdentificationContribuableListCriteria;
import ch.vd.unireg.identification.contribuable.view.IdentificationManuelleMessageEditView;
import ch.vd.unireg.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.unireg.identification.contribuable.view.IdentificationMessagesResultView;
import ch.vd.unireg.identification.contribuable.view.IdentificationMessagesStatsCriteriaView;
import ch.vd.unireg.identification.contribuable.view.NonIdentificationMessageEditView;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.indexer.messageidentification.GlobalMessageIdentificationSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.json.AutoCompleteItem;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.validator.TiersCriteriaValidator;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.WebContextUtils;

@Controller
@RequestMapping(value = "/identification")
@SessionAttributes({"identificationPagination"})
public class IdentificationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationController.class);

	public enum Source {
		enCours,
		suspendu
	}

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec pour la gestion de l'identification de contribuable";
	private static final String ACCESS_DENIED_ACTION_MESSAGE = "Vous ne possédez aucun droit IfoSec procéder à cette action sur un message d'identification de contribuable";
	private static final String ACCESS_DENIED_UNLOCK_MESSAGE = "Vous ne possédez aucun droit IfoSec pour déverouiller un message d'identification de contribuable";
	private static final String ACCESS_DENIED_VISU_MESSAGE = "Vous ne possédez aucun droit IfoSec pour visualiser les messages d'identification de contribuable";
	private static final String TABLE_NAME = "message";
	private static final int PAGE_SIZE = 25;
	private static final String DEFAULT_FIELD = "id";
	private static final String IDENTIFICATION_PAGINATION = "identificationPagination";
	private static final String CTB_SEARCH_CRITERIA_SESSION_NAME = "ctbSearchIdentCriteria";

	private static final WebParamPagination INITIAL_PAGINATION = new WebParamPagination(1, PAGE_SIZE, DEFAULT_FIELD, false);
	private IdentificationMessagesStatsManager identificationMessagesStatsManager;
	private IdentificationMapHelper identificationMapHelper;
	private IdentificationMessagesEditManager identificationMessagesEditManager;
	private SecurityProviderInterface securityProvider;
	private IdentificationMessagesListManager identificationMessagesListManager;
	private TiersMapHelper tiersMapHelper;
	private TiersService tiersService;
	private MessageSource messageSource;

	protected ControllerUtils controllerUtils;

	private IdentificationMessageValidator validator;

	public void setIdentificationMessagesStatsManager(IdentificationMessagesStatsManager identificationMessagesStatsManager) {
		this.identificationMessagesStatsManager = identificationMessagesStatsManager;
	}

	public void setIdentificationMapHelper(IdentificationMapHelper identificationMapHelper) {
		this.identificationMapHelper = identificationMapHelper;
	}

	public void setIdentificationMessagesEditManager(IdentificationMessagesEditManager identificationMessagesEditManager) {
		this.identificationMessagesEditManager = identificationMessagesEditManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setIdentificationMessagesListManager(IdentificationMessagesListManager identificationMessagesListManager) {
		this.identificationMessagesListManager = identificationMessagesListManager;
	}

	public void setValidator(IdentificationMessageValidator validator) {
		this.validator = validator;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 *  Crée un objet ParamPagination initial dans la session
	 *
	 *  Appelé par Spring (grâce à l'annotation) si 'identificationPagination' n'existe pas dans la session
	 *
	 * @return l'objet de pagination initial
	 */
	@ModelAttribute("identificationPagination")
	public ParamPagination initIdentificationPagination() {
		return INITIAL_PAGINATION;
	}

	@RequestMapping(value = "/tableau-bord/effacer.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String effacerFormulaireDeRecherche(ModelMap model) throws Exception {
		setUpModelForStats(model);
		final IdentificationMessagesStatsCriteriaView statsCriteriaView = identificationMessagesStatsManager.getView();
		model.put("statsCriteria", statsCriteriaView);
		model.put("statistiques", identificationMessagesStatsManager.calculerStats(statsCriteriaView));
		return "identification/tableau-bord/stats";
	}

	@RequestMapping(value = "/gestion-messages/effacerEnCours.do",  method = RequestMethod.POST)
	public ModelAndView effacerFormulaireDeRechercheEnCours(HttpServletRequest request,
	                                                           ModelMap model) throws Exception {
		IdentificationContribuableListCriteria criteria = manageCriteria(request, identificationMessagesListManager.getView(null, null, null), "identificationCriteria", false);
		model.put("identificationPagination",INITIAL_PAGINATION);
		return buildResponseForMessageEnCours(request, criteria, model);
	}

	@RequestMapping(value = "/gestion-messages/effacerTraite.do", method = RequestMethod.POST)
	public ModelAndView effacerFormulaireDeRechercheTraite(HttpServletRequest request,ModelMap model) throws Exception {
		IdentificationContribuableListCriteria criteria = manageCriteria(request, identificationMessagesListManager.getView(null, null, null), "identificationCriteria", false);
		return buildReponseForMessageTraite(request, model, criteria);
	}

	@RequestMapping(value = "/gestion-messages/effacerSuspendu.do", method = RequestMethod.POST)
	public ModelAndView effacerFormulaireDeRechercheSuspendu(HttpServletRequest request,ModelMap model) throws Exception {
		IdentificationContribuableListCriteria  criteria = manageCriteria(request, identificationMessagesListManager.getView(null, null, null), "identificationCriteria", false);
		return buildReponseForMessageSuspendu(request, criteria, model);
	}

	@RequestMapping(value = {"/tableau-bord/stats.do"}, method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String getStats(HttpServletRequest request,
	                          @ModelAttribute("statsCriteria") IdentificationMessagesStatsCriteriaView criteriaInSession,
	                          BindingResult bindingResult,
	                          ModelMap model) throws Exception {

		setUpModelForStats(model);
		if (criteriaInSession.getTypeMessage() == null && criteriaInSession.getPeriodeFiscale() == null) {
			criteriaInSession = identificationMessagesStatsManager.getView();
		}
		model.put("statistiques", identificationMessagesStatsManager.calculerStats(criteriaInSession));
		return "identification/tableau-bord/stats";
	}

	@RequestMapping(value = {"/gestion-messages/unlock.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO, Role.SUPERGRA}, accessDeniedMessage = ACCESS_DENIED_UNLOCK_MESSAGE)
	public ModelAndView deverouillerMessage(@RequestParam(value = "source", required = true) Source source,
	                                        @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria) throws Exception {
		deverouillerMessage(criteria);
		return buildModelAndViewFromSource(source);
	}

	@RequestMapping(value = {"/gestion-messages/lock.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO, Role.SUPERGRA}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public ModelAndView verouillerMessage(@RequestParam(value = "source", required = true) Source source,
	                                      @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria) throws Exception {
		verouillerMessage(criteria);
		return buildModelAndViewFromSource(source);
	}

	@RequestMapping(value = {"/gestion-messages/suspendre.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public ModelAndView suspendreMessage(@RequestParam(value = "source", required = true) Source source,
	                                     @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria) throws Exception {
		identificationMessagesListManager.suspendreIdentificationMessages(criteria);
		return buildModelAndViewFromSource(source);
	}

	@RequestMapping(value = {"/gestion-messages/resoumettre.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public ModelAndView reSoumettreMessage(@RequestParam(value = "source", required = true) Source source,
	                                       @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria) throws Exception {
		identificationMessagesListManager.reSoumettreIdentificationMessages(criteria);
		return buildModelAndViewFromSource(source);
	}


	@RequestMapping(value = {"/gestion-messages/demandeEdit.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO, Role.MW_IDENT_CTB_CELLULE_BO, Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public ModelAndView demanderEditionMessage(HttpServletRequest request,
	                                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria identCriteria,
	                                              @RequestParam(value = "id", required = true) Long idMessage,
	                                              @RequestParam(value = "source", required = true) Source source,
	                                              ModelMap model) throws Exception {

		if (!identificationMessagesEditManager.isMessageVerouille(idMessage)) {
			return new ModelAndView("redirect:edit.do?id=" + idMessage+"&source="+source);
		}
		else {
			Flash.warning("le message sélectionné est en cours de traitement par un autre utilisateur");
		}
		final IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(null,null,null);
		return buildResponseFromSource(request, source, criteria, model);
	}

	@RequestMapping(value = "/gestion-messages/edit.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO, Role.MW_IDENT_CTB_CELLULE_BO, Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public String editionMessage(Model model,
	                             HttpSession session,
	                             @RequestParam(value = "id", required = true) Long idMessage,
	                             @RequestParam(value = "source", required = true) Source source) {

		// [SIFISC-13602] tentative de relance de l'identification automatique avant toute chose
		final Long noCtbNouvellementIdentifie = identificationMessagesEditManager.relanceIdentificationAuto(idMessage);
		if (noCtbNouvellementIdentifie != null) {
			Flash.message("Le contribuable a maintenant été identifié automatiquement.");
		}

		// vérouillage (ou pas) du message
		final IdentificationMessagesEditView view = identificationMessagesEditManager.getView(idMessage);
		if (view.getNoCtbIdentifie() == null) {
			identificationMessagesEditManager.verouillerMessage(idMessage);
		}

		final TiersCriteriaView searchCriteria = Optional.of(CTB_SEARCH_CRITERIA_SESSION_NAME)
				.map(session::getAttribute)
				.map(TiersCriteriaView.class::cast)
				.orElseGet(() -> buildPrefilledSearchCriteria(view.getDemandeIdentificationView()));
		if (view.getDemandeIdentificationView().getTypeContribuable() != null) {
			searchCriteria.setTypesTiersImperatifs(Collections.singleton(view.getDemandeIdentificationView().getTypeContribuable()));
		}

		if (view.getNoCtbIdentifie() == null) {
			model.addAttribute("found", searchTiers(searchCriteria, error -> model.addAttribute("searchCtbErrorMessage", error)));
		}
		return showSearchForm(model, source, view, searchCriteria);
	}

	private List<TiersIndexedDataView> searchTiers(TiersCriteriaView criteria, Consumer<String> errorConsumer) {
		final String message;
		try {
			final List<TiersIndexedData> coreFound = tiersService.search(criteria.asCore());
			return coreFound.stream()
					.map(TiersIndexedDataView::new)
					.collect(Collectors.toList());
		}
		catch (TooManyResultsIndexerException ee) {
			if (ee.getNbResults() > 0) {
				message = messageSource.getMessage("error.preciser.recherche.trouves", new Object[] {String.valueOf(ee.getNbResults())}, WebContextUtils.getDefaultLocale());
			}
			else {
				message = messageSource.getMessage("error.preciser.recherche", null, WebContextUtils.getDefaultLocale());
			}
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			message = messageSource.getMessage("error.recherche", null, WebContextUtils.getDefaultLocale());
		}
		errorConsumer.accept(message);
		return null;
	}

	private TiersCriteriaView buildPrefilledSearchCriteria(DemandeIdentificationView demandeView) {
		final TiersCriteriaView view = new TiersCriteriaView();
		view.setNumeroAVS(Optional.ofNullable(demandeView.getNavs13()).orElse(demandeView.getNavs11()));
		view.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		view.setNomRaison(Stream.of(demandeView.getNom(), demandeView.getPrenoms())
				                  .filter(StringUtils::isNotBlank)
				                  .collect(Collectors.joining(" ")));
		// [SIFISC-130] La date de naissance ne fait pas partie des critères à pré-remplir
		//view.setDateNaissanceInscriptionRC(demandeView.getDateNaissance());
		return view;
	}

	private String showSearchForm(Model model,
	                              Source source,
	                              IdentificationMessagesEditView editView,
	                              TiersCriteriaView searchView) {

		// visualisation complète ou limitée...
		final TiersCriteria.TypeVisualisation typeVisualisation;
		if (SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			typeVisualisation = TiersCriteria.TypeVisualisation.COMPLETE;
		}
		else if (SecurityHelper.isGranted(securityProvider, Role.VISU_LIMITE)) {
			typeVisualisation = TiersCriteria.TypeVisualisation.LIMITEE;
		}
		else {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg.");
		}
		searchView.setTypeVisualisation(typeVisualisation);

		final DemandeIdentificationView demandeView = editView.getDemandeIdentificationView();
		model.addAttribute("message", editView);
		model.addAttribute("messageData", demandeView);
		model.addAttribute("source", source);
		model.addAttribute("typesRechercheNom", tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute("formesJuridiquesEnum", tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.addAttribute("categoriesEntreprisesEnum", tiersMapHelper.getMapCategoriesEntreprise());
		model.addAttribute("identificationSearchCriteria", searchView);
		model.addAttribute("hideSoumissionExpertise", demandeView.getTypeDemande() == TypeDemande.RAPPROCHEMENT_RF);
		model.addAttribute("hideNonIdentifiable", demandeView.getTypeDemande() == TypeDemande.RAPPROCHEMENT_RF);

		return "identification/identification";
	}

	@RequestMapping(value = "/gestion-messages/edit.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO, Role.MW_IDENT_CTB_CELLULE_BO, Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public String rechercheContribuable(Model model,
	                                    @Valid @ModelAttribute(value = "identificationSearchCriteria") TiersCriteriaView criteria,
	                                    BindingResult bindingResult,
	                                    @RequestParam(value = "source", required = true) Source source,
	                                    @RequestParam(value = "id", required = true) Long idMessage,
                                        HttpSession session) {
		if (bindingResult.hasErrors()) {
			return showSearchForm(model,
			                      source,
			                      identificationMessagesEditManager.getView(idMessage),
			                      criteria);
		}

		session.setAttribute(CTB_SEARCH_CRITERIA_SESSION_NAME, criteria);
		return "redirect:edit.do?source=" + source + "&id=" + idMessage;
	}

	@RequestMapping(value = "/gestion-messages/back-from-edit.do", method = RequestMethod.GET)
	public ModelAndView retourApresEdition(HttpSession session,
	                                       @RequestParam(value = "idToUnlock", required = false) Long idMessageToUnlock,
	                                       @RequestParam(value = "source", required = true) Source source) {
		if (idMessageToUnlock != null) {
			identificationMessagesEditManager.deVerouillerMessage(idMessageToUnlock, false);
		}
		session.removeAttribute(CTB_SEARCH_CRITERIA_SESSION_NAME);
		return buildModelAndViewFromSource(source);
	}

	@RequestMapping(value = "/gestion-messages/reset-search.do", method = RequestMethod.GET)
	public String effacerCritereRechercheContribuable(HttpSession session,
	                                                  @RequestParam(value = "id", required = true) Long idMessage,
	                                                  @RequestParam(value = "source", required = true) Source source) {
		session.removeAttribute(CTB_SEARCH_CRITERIA_SESSION_NAME);
		return "redirect:edit.do?source=" + source + "&id=" + idMessage;
	}

	@RequestMapping(value = "/gestion-messages/soumettre-expertise.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.LISTE_IS_IDENT_CTB_CELLULE_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.MW_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public String soumettreExpertise(@RequestParam(value = "id", required = true) Long idMessage,
	                                 @RequestParam(value = "source", required = true) Source source) {

		final IdentificationMessagesEditView aModifier = identificationMessagesEditManager.getView(idMessage);
		if (!aModifier.getDemandeIdentificationView().getEtatMessage().isEncoreATraiter()) {
			Flash.warning("Ce message a déjà été traité, vous avez été redirigé vers la liste des messages");
		}
		else {
			identificationMessagesEditManager.expertiser(idMessage);
		}
		return "redirect:back-from-edit.do?idToUnlock=" + idMessage + "&source=" + source;
	}

	@RequestMapping(value = "/gestion-messages/non-identifie.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public String nonIndenfication(Model model,
	                               @RequestParam(value = "id", required = true) Long idMessage,
	                               @RequestParam(value = "source", required = true) Source source) {

		// verrouilage du message (ne l'est-il pas déjà ?)
		identificationMessagesEditManager.verouillerMessage(idMessage);
		return showConfirmationNonIdentification(model, idMessage, source, new NonIdentificationMessageEditView());
	}

	private String showConfirmationNonIdentification(Model model,
	                                                 Long idMessage,
	                                                 Source source,
	                                                 NonIdentificationMessageEditView view) {

		// remplissage du modèle pour affichage de page de confirmation (avec choix du motif)
		final IdentificationMessagesEditView editView = identificationMessagesEditManager.getView(idMessage);
		model.addAttribute("messageData", editView.getDemandeIdentificationView());
		model.addAttribute("source", source);
		model.addAttribute("erreursMessage", identificationMapHelper.initErreurMessage());
		model.addAttribute("nonIdentification", view);

		return "identification/gestion-messages/nonIdentifie";
	}

	@RequestMapping(value = "/gestion-messages/non-identifie.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public String confirmeNonIdentification(Model model,
	                                        @Valid @ModelAttribute(value = "nonIdentification") NonIdentificationMessageEditView view,
	                                        BindingResult bindingResult,
	                                        @RequestParam(value = "id", required = true) Long idMessage,
	                                        @RequestParam(value = "source", required = true) Source source) {

		if (bindingResult.hasErrors()) {
			return showConfirmationNonIdentification(model, idMessage, source, view);
		}

		final IdentificationMessagesEditView aModifier = identificationMessagesEditManager.getView(idMessage);
		if (!aModifier.getDemandeIdentificationView().getEtatMessage().isEncoreATraiter()) {
			Flash.warning("Ce message a déjà été traité, vous avez été redirigé vers la liste des messages");
		}
		else {
			identificationMessagesEditManager.impossibleAIdentifier(idMessage, view.getErreurMessage());
		}
		return "redirect:back-from-edit.do?idToUnlock=" + idMessage + "&source=" + source;
	}

	@RequestMapping(value = "/gestion-messages/identifie.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_GEST_BO, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO, Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	public String identifie(@RequestParam(value = "id", required = true) Long idMessage,
	                        @RequestParam(value = "source", required = true) Source source,
	                        @ModelAttribute(value = "identificationSelect") IdentificationManuelleMessageEditView donneeIdentification,
	                        BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return "redirect:edit.do?id=" + idMessage + "&source=" + source;
		}

		final IdentificationMessagesEditView aModifier = identificationMessagesEditManager.getView(idMessage);
		if (!aModifier.getDemandeIdentificationView().getEtatMessage().isEncoreATraiter()) {
			Flash.warning("Ce message a déjà été traité, vous avez été redirigé vers la liste des messages");
		}
		else {
			final IdentificationContribuable.Etat etat;
			if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_GEST_BO, Role.MW_IDENT_CTB_ADMIN)) {
				etat = IdentificationContribuable.Etat.TRAITE_MAN_EXPERT;
			}
			else if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_CELLULE_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO, Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB)) {
				etat = IdentificationContribuable.Etat.TRAITE_MANUELLEMENT;
			}
			else {
				throw new AccessDeniedException(ACCESS_DENIED_ACTION_MESSAGE);
			}
			identificationMessagesEditManager.forceIdentification(idMessage, donneeIdentification.getContribuableIdentifie(), etat);
		}
		return "redirect:back-from-edit.do?idToUnlock=" + idMessage + "&source=" + source;
	}

	private ModelAndView buildResponseFromSource(HttpServletRequest request, Source source, IdentificationContribuableListCriteria criteria, ModelMap model) throws AdressesResolutionException {
		if (source == Source.enCours) {
			return buildResponseForMessageEnCours(request, criteria, model);
		}
		else if (source == Source.suspendu) {
			return buildReponseForMessageSuspendu(request, criteria, model);
		}

		throw new IllegalArgumentException("Invalid value for source parameter");
	}

	private ModelAndView buildModelAndViewFromSource(Source source) {
		switch (source) {
		case enCours:
			return new ModelAndView("redirect:/identification/gestion-messages/listEnCours.do");
		case suspendu:
			return new ModelAndView("redirect:/identification/gestion-messages/listSuspendu.do");
		default:
			throw new IllegalArgumentException("Invalid value for source parameter");
		}
	}

	@RequestMapping(value = "/gestion-messages/listEnCours.do", method =  {RequestMethod.POST, RequestMethod.GET})
	public String retourSurLaListeEnCours(@ModelAttribute("identificationPagination") ParamPagination paginationInSession ) throws AdresseException {
		// Redirect vers nav-list.do  avec en parametre la pagination en session
		return buildNavListRedirect(paginationInSession,"nav-listEnCours.do");
	}

	@RequestMapping(value = {"/gestion-messages/nav-listEnCours.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO,
			Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	public ModelAndView listerEncours(HttpServletRequest request,
	                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                              BindingResult bindingResult,
	                              @RequestParam(value = "wipeCriteria", required = false) String wipeCriteria,
	                              ModelMap model) throws AdressesResolutionException {

		if (bindingResult.hasErrors()) {
			// erreur dans les critères de recherche : on réaffiche le formulaire sans résultat (SIFISC-14788)
			model.put("identificationCriteria", criteria);
			model.put("tailleTableau", 0);
			setUpModelForListMessageEnCours(model);
			return new ModelAndView("identification/gestion-messages/list", model);
		}

		// [SIFISC-27085] on ne veut jamais recharger le criteria de la session lorsqu'on soumet (POST) le formulaire
		final boolean keepCriteria = (wipeCriteria == null && "GET".equals(request.getMethod()));
		criteria = manageCriteria(request, criteria, "identificationCriteria", keepCriteria);
		criteria.setUserCourant(AuthenticationHelper.getCurrentPrincipal());

		addPaginationToModel(request, model);
		return buildResponseForMessageEnCours(request, criteria, model);
	}

	private void addPaginationToModel(HttpServletRequest request, ModelMap model) {
		final ParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, DEFAULT_FIELD, false);
		model.put(IDENTIFICATION_PAGINATION,pagination);
	}

	/**
	 * Permet de conserver les critères sélectionneées des messages dans la session
	 * @param request
	 * @param criteria
	 * @param criteriaName
	 * @param keepCriteria
	 */
	private IdentificationContribuableListCriteria manageCriteria(HttpServletRequest request, IdentificationContribuableListCriteria criteria, String criteriaName, boolean keepCriteria) {
		if (criteria.isEmpty()) {
			IdentificationContribuableListCriteria savedCriteria = (IdentificationContribuableListCriteria) request.getSession().getAttribute(criteriaName);
			if (savedCriteria != null && !savedCriteria.isEmpty() && keepCriteria) {
				criteria = savedCriteria;
			}
		}
		request.getSession().setAttribute(criteriaName,criteria);
		return criteria;
	}

	@RequestMapping(value = "/gestion-messages/listSuspendu.do", method =  {RequestMethod.POST, RequestMethod.GET})
	public String retourSurLaListeSuspendu(@ModelAttribute("identificationPagination") ParamPagination paginationInSession ) throws AdresseException {
		// Redirect vers nav-list.do  avec en parametre la pagination en session
		return buildNavListRedirect(paginationInSession,"nav-listSuspendu.do");
	}

	@RequestMapping(value = {"/gestion-messages/nav-listSuspendu.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_GEST_BO, Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	public ModelAndView listerSuspendu(HttpServletRequest request,
	                                     @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                                     BindingResult bindingResult,
	                                     @RequestParam(value = "wipeCriteria", required = false) String wipeCriteria,
	                                     ModelMap model) throws AdressesResolutionException {

		if (bindingResult.hasErrors()) {
			// erreur dans les critères de recherche : on réaffiche le formulaire sans résultat (SIFISC-14788)
			model.put("identificationCriteria", criteria);
			model.put("tailleTableau", 0);
			setUpModelForListMessageSuspendu(model);
			return new ModelAndView("identification/gestion-messages/list", model);
		}

		// [SIFISC-27085] on ne veut jamais recharger le criteria de la session lorsqu'on soumet (POST) le formulaire
		final boolean keepCriteria = (wipeCriteria == null && "GET".equals(request.getMethod()));
		criteria = manageCriteria(request, criteria, "identificationCriteria", keepCriteria);
		addPaginationToModel(request, model);
		return buildReponseForMessageSuspendu(request, criteria, model);
	}


	@RequestMapping(value = {"/gestion-messages/listEnCoursFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO,
			Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	public ModelAndView listerEnCoursFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = false) IdentificationContribuable.Etat etat,
	                                              @RequestParam(value = "periode", required = false) Integer periode,
	                                              @RequestParam(value = "typeMessage", required = false) String typeMessage,
	                                              ModelMap model) throws AdressesResolutionException {

		setUpModelForListMessageEnCours(model);

		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(typeMessage, periode, etat);
		model.put("identificationCriteria", criteria);
		construireModelMessageEnCours(request, model, criteria);
		criteria = manageCriteria(request, criteria, "identificationCriteria", true);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	@RequestMapping(value = {"/gestion-messages/listSuspenduFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_CELLULE_BO, Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	public ModelAndView listerSuspenduFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = false) IdentificationContribuable.Etat etat,
	                                              @RequestParam(value = "periode", required = false) Integer periode,
	                                              @RequestParam(value = "typeMessage", required = false) String typeMessage,
	                                              ModelMap model) throws AdressesResolutionException {

		setUpModelForListMessageSuspendu(model);

		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(typeMessage, periode, etat);
		model.put("identificationCriteria", criteria);
		construireModelMessageSuspendu(request, model, criteria);
		criteria = manageCriteria(request, criteria, "identificationCriteria", true);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	@RequestMapping(value = "/gestion-messages/listTraite.do", method =  {RequestMethod.POST, RequestMethod.GET})
	public String retourSurLaListeTraite(@ModelAttribute("identificationPagination") ParamPagination paginationInSession ) throws AdresseException {
		// Redirect vers nav-list.do  avec en parametre la pagination en session
		return buildNavListRedirect(paginationInSession,"nav-listTraite.do");
	}

	@RequestMapping(value = {"/gestion-messages/nav-listTraite.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO,
			Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	public ModelAndView listerTraite(HttpServletRequest request,
	                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                              BindingResult bindingResult,
	                              @RequestParam(value = "wipeCriteria", required = false) String wipeCriteria,
	                              ModelMap model) throws AdressesResolutionException {

		if (bindingResult.hasErrors()) {
			// erreur dans les critères de recherche : on réaffiche le formulaire sans résultat (SIFISC-14788)
			model.put("identificationCriteria", criteria);
			model.put("tailleTableau", 0);
			setUpModelForListMessageTraites(model);
			return new ModelAndView("identification/gestion-messages/list", model);
		}

		// [SIFISC-27085] on ne veut jamais recharger le criteria de la session lorsqu'on soumet (POST) le formulaire
		final boolean keepCriteria = (wipeCriteria == null && "GET".equals(request.getMethod()));
		criteria = manageCriteria(request, criteria, "identificationCriteria", keepCriteria);
		addPaginationToModel(request, model);
		return buildReponseForMessageTraite(request, model, criteria);
	}


	@RequestMapping(value = {"/gestion-messages/listTraiteFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO,
			Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	public ModelAndView listerTraiteFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = false) IdentificationContribuable.Etat etat,
	                                              @RequestParam(value = "periode", required = false) Integer periode,
	                                              @RequestParam(value = "typeMessage", required = false) String typeMessage,
	                                              ModelMap model) throws AdressesResolutionException {

		setUpModelForListMessageTraites(model);

		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(typeMessage, periode, etat);
		model.put("identificationCriteria", criteria);
		construireModelMessageTraite(request, model, criteria);
		criteria = manageCriteria(request, criteria, "identificationCriteria", true);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	/**Construit le model est retourne le modelAndView de réponse des messages traites
	 *
	 * @param request
	 * @param model
	 * @param criteria
	 * @return
	 * @throws AdressesResolutionException
	 */
	private ModelAndView buildReponseForMessageTraite(HttpServletRequest request, ModelMap model, IdentificationContribuableListCriteria criteria) throws AdressesResolutionException {
		setUpModelForListMessageTraites(model);
		model.put("identificationCriteria", criteria);
		construireModelMessageTraite(request, model, criteria);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	/**
	 * Construit le model est retourne le modelAndView de réponse des messages en cours
	 * @param request
	 * @param criteria
	 * @param model
	 * @return
	 * @throws AdressesResolutionException
	 */
	private ModelAndView buildResponseForMessageEnCours(HttpServletRequest request, IdentificationContribuableListCriteria criteria, ModelMap model) throws AdressesResolutionException {
		setUpModelForListMessageEnCours(model);
		model.put("identificationCriteria", criteria);
		// Récupération de la pagination
		construireModelMessageEnCours(request, model, criteria);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	/**
	 * Construit le model est retourne le modelAndView de réponse des messagesSuspendu
	 * @param request
	 * @param criteria
	 * @param model
	 * @return
	 * @throws AdressesResolutionException
	 */
	private ModelAndView buildReponseForMessageSuspendu(HttpServletRequest request, IdentificationContribuableListCriteria criteria, ModelMap model) throws AdressesResolutionException {
		setUpModelForListMessageSuspendu(model);
		model.put("identificationCriteria", criteria);
		// Récupération de la pagination
		construireModelMessageSuspendu(request, model, criteria);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	private void construireModelMessageEnCours(HttpServletRequest request, ModelMap model, IdentificationContribuableListCriteria criteria) throws AdressesResolutionException {
		// Récupération de la pagination
		//final WebParamPagination pagination = new WebParamPagination(request, "message", 25);
		final WebParamPagination paginationStockee = (WebParamPagination)model.get(IDENTIFICATION_PAGINATION);
		final WebParamPagination pagination = paginationStockee !=null? paginationStockee: new WebParamPagination(request, "message", 25);
		final List<IdentificationMessagesResultView> listIdentifications;
		final int nombreElements;
		final TypeDemande types[] = getAllowedTypes();

		if (SecurityHelper.isGranted(securityProvider, Role.MW_IDENT_CTB_ADMIN)) {
			listIdentifications = identificationMessagesListManager.find(criteria, pagination, IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION);
			nombreElements = identificationMessagesListManager.count(criteria, IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION);
		}
		else if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_VISU,Role.MW_IDENT_CTB_GEST_BO)) {
			listIdentifications = identificationMessagesListManager.find(criteria, pagination, IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES,types);
			nombreElements = identificationMessagesListManager.count(criteria, IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES,types);
		}
		else {//Cellule back office

			listIdentifications = identificationMessagesListManager.findEncoursSeul(criteria, pagination, types);
			nombreElements = identificationMessagesListManager.countEnCoursSeul(criteria, types);
		}
		final int tailleTableau = nombreElements > GlobalMessageIdentificationSearcher.MAX_RESULTS?GlobalMessageIdentificationSearcher.MAX_RESULTS:nombreElements;
		model.put("tailleTableau", tailleTableau);
		model.put("identifications", listIdentifications);
		model.put("identificationsSize", nombreElements);
	}

	private void construireModelMessageSuspendu(HttpServletRequest request, ModelMap model, IdentificationContribuableListCriteria criteria) throws AdressesResolutionException {
		// Récupération de la pagination
		final WebParamPagination paginationStockee = (WebParamPagination)model.get(IDENTIFICATION_PAGINATION);
		final WebParamPagination pagination = paginationStockee !=null? paginationStockee: new WebParamPagination(request, "message", 25);
		final List<IdentificationMessagesResultView> listIdentifications;
		final int nombreElements;
		listIdentifications = identificationMessagesListManager.find(criteria, pagination, IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS);
		nombreElements = identificationMessagesListManager.count(criteria, IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS);
		final int tailleTableau = nombreElements > GlobalMessageIdentificationSearcher.MAX_RESULTS?GlobalMessageIdentificationSearcher.MAX_RESULTS:nombreElements;
		model.put("tailleTableau", tailleTableau);
		model.put("identifications", listIdentifications);
		model.put("identificationsSize", nombreElements);
	}


	private void construireModelMessageTraite(HttpServletRequest request, ModelMap model, IdentificationContribuableListCriteria criteria) throws AdressesResolutionException {
		// Récupération de la pagination
		final WebParamPagination paginationStockee = (WebParamPagination)model.get(IDENTIFICATION_PAGINATION);
		final WebParamPagination pagination = paginationStockee !=null? paginationStockee: new WebParamPagination(request, "message", 25);
		final List<IdentificationMessagesResultView> listIdentifications;
		int nombreElements;

		final TypeDemande types[] = getAllowedTypes();
		listIdentifications = identificationMessagesListManager.find(criteria, pagination, IdentificationContribuableEtatFilter.SEULEMENT_TRAITES, types);
		nombreElements = identificationMessagesListManager.count(criteria, IdentificationContribuableEtatFilter.SEULEMENT_TRAITES, types);
		final int tailleTableau = nombreElements > GlobalMessageIdentificationSearcher.MAX_RESULTS?GlobalMessageIdentificationSearcher.MAX_RESULTS:nombreElements;
		model.put("tailleTableau", tailleTableau);
		model.put("identifications", listIdentifications);
		model.put("identificationsSize", nombreElements);
	}

	private void setUpModelForStats(ModelMap model) throws Exception {
		model.put("typesMessage", identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.TOUS));
		model.put("periodesFiscales", identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.TOUS));
	}

	private void setUpModelForListMessageEnCours(ModelMap model) {
		if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_ADMIN)) {
			setUpModelForListMessageEnCoursAvecException(model);
		}
		else{
			setUpModelForListMessageEnCoursNormal(model);
		}
	}

	/**
	 * Retourne les émetteurs qui correspondent aux critères spécifiés sous forme JSON (voir http://blog.springsource.com/2010/01/25/ajax-simplifications-in-spring-3-0/)
	 *
	 * @param term   un critère de recherche texte
	 * @param filter un filtre sur les types d'émetteur
	 * @return les émetteurs trouvés
	 */
	@RequestMapping(value = "/gestion-messages/autocompleteEmetteurs.do", method = RequestMethod.GET)
	@ResponseBody
	public List<AutoCompleteItem> security(@RequestParam("term") String term, @RequestParam("filter") IdentificationContribuableEtatFilter filter) throws Exception {

		final Map<String, String> emetteursMap = identificationMapHelper.initMapEmetteurId(filter);
		return emetteursMap.entrySet().stream()
				.filter(e -> e.getValue().toLowerCase().contains(term.toLowerCase()))
				.map(e -> new AutoCompleteItem(e.getValue(), e.getValue(), e.getKey()))
				.collect(Collectors.toList());
	}

	private void setUpModelForListMessageEnCoursNormal(ModelMap model) {
		model.put("typesMessage", initMapTypeMessageEncours());
		model.put("emetteursFilter", IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);
		model.put("priorites", identificationMapHelper.initMapPrioriteEmetteur());
		model.put("periodesFiscales",identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES));
		model.put("etatsMessage", identificationMapHelper.initMapEtatMessageEnCours());
		model.put("messageEnCours", true);
		model.put("messageTraite", false);
		model.put("messageSuspendu", false);
	}

	private void setUpModelForListMessageEnCoursAvecException(ModelMap model) {
		model.put("typesMessage", initMapTypeMessageEncoursEtException());
		model.put("emetteursFilter", IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION);
		model.put("priorites", identificationMapHelper.initMapPrioriteEmetteur());
		model.put("periodesFiscales",identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION));
		model.put("etatsMessage", identificationMapHelper.initMapEtatMessageEnCoursEtException());
		model.put("messageEnCours", true);
		model.put("messageTraite", false);
		model.put("messageSuspendu", false);
	}

	private void setUpModelForListMessageSuspendu(ModelMap model) {
		model.put("typesMessage",initMapTypeMessageSuspendu());
		model.put("emetteursFilter", IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS);
		model.put("priorites", identificationMapHelper.initMapPrioriteEmetteur());
		model.put("periodesFiscales", identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS));
		model.put("etatsMessage", identificationMapHelper.initMapEtatMessageSuspendu());
		model.put("messageEnCours", false);
		model.put("messageTraite", false);
		model.put("messageSuspendu", true);
	}

	private void setUpModelForListMessageTraites(ModelMap model) {
		model.put("typesMessage", initMapTypeMessageTraite());
		model.put("periodesFiscales", identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES));
		model.put("emetteursFilter", IdentificationContribuableEtatFilter.SEULEMENT_TRAITES);
		model.put("priorites", identificationMapHelper.initMapPrioriteEmetteur());
		model.put("etatsMessage", identificationMapHelper.initMapEtatMessageArchive());
		model.put("traitementUsers", identificationMapHelper.initMapUser());
		model.put("messageEnCours", false);
		model.put("messageTraite", true);
		model.put("messageSuspendu", false);
	}

	/**
	 * Retourne les types de demandes autorisés par rapports aux droits du principal courant
	 *
	 * @return un tableau (potentiellement vide...) des types autorisés
	 */
	protected TypeDemande[] getAllowedTypes() {
		final Set<TypeDemande> types = EnumSet.noneOf(TypeDemande.class);
		if (SecurityHelper.isGranted(securityProvider, Role.MW_IDENT_CTB_ADMIN)) {
			types.add(TypeDemande.MELDEWESEN);
			types.add(TypeDemande.NCS);
			types.add(TypeDemande.IMPOT_SOURCE);
			types.add(TypeDemande.RAPPROCHEMENT_RF);
		}
		if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_CELLULE_BO, Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_GEST_BO)) {
			types.add(TypeDemande.MELDEWESEN);
			types.add(TypeDemande.NCS);
			types.add(TypeDemande.IMPOT_SOURCE);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.NCS_IDENT_CTB_CELLULE_BO)) {
			types.add(TypeDemande.NCS);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.LISTE_IS_IDENT_CTB_CELLULE_BO)) {
			types.add(TypeDemande.IMPOT_SOURCE);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.RAPPROCHEMENT_RF_IDENTIFICATION_CTB)) {
			types.add(TypeDemande.RAPPROCHEMENT_RF);
		}

		if (types.isEmpty()) {
			// aucun droit... plutôt que tous
			return new TypeDemande[0];
		}
		else {
			return types.toArray(new TypeDemande[0]);
		}
	}

	protected Map<String, String> initMapTypeMessageEncours() {
		final TypeDemande[] types = getAllowedTypes();
		return identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES, types);
	}

	protected Map<String, String> initMapTypeMessageEncoursEtException() {
		final TypeDemande[] types = getAllowedTypes();
		return identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION, types);
	}

	protected Map<String, String> initMapTypeMessageSuspendu() {
		final TypeDemande[] types = getAllowedTypes();
		return identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS, types);
	}

	protected Map<Integer, String> initMapPeriodeFiscaleMessageEncours() {
		return identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);
	}

	protected Map<Demande.PrioriteEmetteur, String> initMapPrioriteEmetteurMessageEncours() {
		return identificationMapHelper.initMapPrioriteEmetteur();
	}

	protected Map<String, String> initMapTypeMessageTraite() {
		final TypeDemande[] types = getAllowedTypes();
		return identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.SEULEMENT_TRAITES, types);
	}

	protected Map<Integer, String> initMapPeriodeFiscaleTraite() {
		return identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_TRAITES);
	}

	protected Map<Demande.PrioriteEmetteur, String> initMapPrioriteEmetteurTraite() {
		return identificationMapHelper.initMapPrioriteEmetteur();
	}

	@InitBinder("identificationCriteria")
	protected void initBinderIdentificationCriteria(HttpServletRequest request, WebDataBinder binder) {
		binder.setValidator(validator);
		Locale locale = request.getLocale();
		SimpleDateFormat sdf = new SimpleDateFormat(DateHelper.DATE_FORMAT_DISPLAY, locale);
		sdf.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
		NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(true);
		binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true));
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(Set.class, true));
		binder.registerCustomEditor(boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(RegDate.class, "dateNaissance", new RegDateEditor(true, true, false));
	}

	@InitBinder("message")
	protected void initBinderMessage(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false));
	}

	@InitBinder("identificationSearchCriteria")
	protected void initBinderCtbSearchCriteria(WebDataBinder binder) {
		binder.setValidator(new TiersCriteriaValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false));
	}

	@InitBinder("nonIdentification")
	protected void initBinderNonIdentification(WebDataBinder binder) {
		binder.setValidator(new NonIdentificationMessageEditValidator());
	}

	@InitBinder("identificationSelect")
	protected void initBinderSelectIdentification(WebDataBinder binder) {
		binder.setValidator(new IdentificationManuelleMessageEditValidator());
	}

	private void deverouillerMessage(IdentificationContribuableListCriteria criteria) throws Exception {
		final Long[] tabIdsMessages = criteria.getTabIdsMessages();
		if (tabIdsMessages != null) {
			for (final Long tabIdsMessage : tabIdsMessages) {
				identificationMessagesEditManager.deVerouillerMessage(tabIdsMessage, true);
			}
		}
	}

	private void verouillerMessage(IdentificationContribuableListCriteria criteria) throws Exception {
		final Long[] tabIdsMessages = criteria.getTabIdsMessages();
		if (tabIdsMessages != null) {
			for (final Long tabIdsMessage : tabIdsMessages) {
				identificationMessagesEditManager.verouillerMessage(tabIdsMessage);
			}
		}
	}

	public static void calculerView(HttpServletRequest request, ModelAndView mav, String source_param) {
		final String source = (String) request.getSession().getAttribute(source_param);
		final Source src = Source.valueOf(source);
		switch (src) {
			case enCours:
				mav.setView(new RedirectView("listEnCours.do?keepCriteria=true"));
				break;
			case suspendu:
				mav.setView(new RedirectView("listSuspendu.do?keepCriteria=true"));
				break;
		}
	}

	private String buildNavListRedirect(ParamPagination pagination,String action) {
		return buildNavListRedirect(pagination, TABLE_NAME, String.format("/identification/gestion-messages/%s",action));
	}


	protected String buildNavListRedirect(ParamPagination pagination, final String tableName, final String navListPath) {
		String displayTagParameter = controllerUtils.getDisplayTagRequestParametersForPagination(tableName, pagination);
		if (displayTagParameter != null) {
			displayTagParameter = "?" + displayTagParameter;
		}
		return ("redirect:" + navListPath + displayTagParameter);
	}

}
