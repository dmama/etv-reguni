package ch.vd.unireg.rapport;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.common.URLHelper;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.rapport.manager.RapportEditManager;
import ch.vd.unireg.rapport.view.RapportListView;
import ch.vd.unireg.rapport.view.RapportView;
import ch.vd.unireg.rapport.view.SetPrincipalView;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportEntreTiersDAO;
import ch.vd.unireg.tiers.RapportEntreTiersKey;
import ch.vd.unireg.tiers.RepresentationLegale;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.validator.RapportEditValidator;
import ch.vd.unireg.tiers.validator.TiersCriteriaValidator;
import ch.vd.unireg.tiers.view.DebiteurView;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/rapport")
public class RapportController {

	private final Logger LOGGER = LoggerFactory.getLogger(RapportController.class);

	private TiersDAO tiersDAO;
	private RapportEntreTiersDAO rapportEntreTiersDAO;

	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceCivilCacheWarmer cacheWarmer;
	private TiersMapHelper tiersMapHelper;
	private MessageSource messageSource;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;
	private AutorisationManager autorisationManager;
	private RapportEditManager rapportEditManager;
	private Validator rapportAddValidator;
	private RapportEditValidator rapportEditValidator;
	private Validator setPrincipalValidator;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheWarmer(ServiceCivilCacheWarmer cacheWarmer) {
		this.cacheWarmer = cacheWarmer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setRapportEditManager(RapportEditManager rapportEditManager) {
		this.rapportEditManager = rapportEditManager;
	}

	public void setRapportAddValidator(Validator rapportAddValidator) {
		this.rapportAddValidator = rapportAddValidator;
	}

	public void setRapportEditValidator(RapportEditValidator rapportEditValidator) {
		this.rapportEditValidator = rapportEditValidator;
	}

	public void setSetPrincipalValidator(Validator setPrincipalValidator) {
		this.setPrincipalValidator = setPrincipalValidator;
	}

	private Set<RapportEntreTiersKey> getAllowedTypes() {
		final Set<RapportEntreTiersKey> types;
		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			types = RapportHelper.ALLOWED_VISU_LIMITEE;
		}
		else {
			types = RapportHelper.ALLOWED_VISU_COMPLETE;
		}
		return types;
	}

	private static Set<RapportEntreTiersKey> buildTypeSet(@Nullable TypeRapportEntreTiers askedFor, Set<RapportEntreTiersKey> allowed) {
		final Set<RapportEntreTiersKey> types = new HashSet<>(allowed);
		if (askedFor != null) {
			types.removeIf(key -> key.getType() != askedFor);
		}
		return types.isEmpty() ? allowed : types;
	}

	/**
	 * Affiche qui liste les rapports-entre-tiers d'un tiers pour l'édition.
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String list(@RequestParam("id") long tiersId, Model model) throws AdresseException {

		// checks de sécurité
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final TiersEditView tiersView = rapportEditManager.getView(tiersId);
		model.addAttribute("command", tiersView);

		return "tiers/edition/rapport/list";
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder(value = "searchView")
	public void initBinderForSearch(WebDataBinder binder) {
		binder.addValidators(new TiersCriteriaValidator(true));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Affiche l'écran de recherche d'un tiers à lier par un rapport-entre-tiers.
	 */
	@RequestMapping(value = "/add-search.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addSearch(@Valid @ModelAttribute("searchView") final RapportListView view, BindingResult binding, Model model) {

		final long tiersId = view.getTiersId();

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		// checks de sécurité
		controllerUtils.checkAccesDossierEnLecture(tiersId);
		if (!autorisationManager.isEditAllowed(tiers)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit d'édition des rapports-entre-tiers sur ce tiers");
		}

		model.addAttribute("typesRechercheNom", tiersMapHelper.getMapTypeRechercheNom());

		if (binding.hasErrors() || view.isEmpty()) {
			return "tiers/edition/rapport/add-search";
		}

		try {
			final List<TiersIndexedDataView> list = tiersService.search(view.asCore()).stream()
					.map(TiersIndexedDataView::new)
					.collect(Collectors.toList());
			model.addAttribute("list", list);
		}
		catch (TooManyResultsIndexerException ee) {
			if (ee.getNbResults() > 0) {
				binding.reject("error.preciser.recherche.trouves", new Object[]{String.valueOf(ee.getNbResults())}, null);
			}
			else {
				binding.reject("error.preciser.recherche");
			}
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			binding.reject("error.recherche");
		}

		return "tiers/edition/rapport/add-search";
	}

	/**
	 * Affichage de l'écran de création d'un nouveau rapport-entre-tiers.
	 *
	 * @param numeroTiers    le tiers de départ
	 * @param numeroTiersLie le tiers lié
	 */
	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String add(@RequestParam("numeroTiers") long numeroTiers, @RequestParam("numeroTiersLie") long numeroTiersLie, Model model) throws AdressesResolutionException {

		final Tiers tiers = tiersDAO.get(numeroTiers);
		if (tiers == null) {
			throw new TiersNotFoundException(numeroTiers);
		}
		final Tiers tiersLie = tiersDAO.get(numeroTiersLie);
		if (tiersLie == null) {
			throw new TiersNotFoundException(numeroTiersLie);
		}

		// checks de sécurité
		controllerUtils.checkAccesDossierEnLecture(numeroTiers);
		controllerUtils.checkAccesDossierEnLecture(numeroTiersLie);
		if (!autorisationManager.isEditAllowed(tiers)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit d'édition des rapports-entre-tiers sur le tiers n°" + numeroTiers);
		}

		//vérification des droits de création de rapport entre tiers non travail par rapportEditManager
		final RapportView rapportAddView = rapportEditManager.get(numeroTiers, numeroTiersLie);
		model.addAttribute("rapportAddView", rapportAddView);

		return "tiers/edition/rapport/add";
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder(value = "rapportAddView")
	public void initBinderForAdd(WebDataBinder binder) {
		binder.addValidators(rapportAddValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Méthode de création d'un nouveau rapport-entre-tiers.
	 */
	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String add(@ModelAttribute("rapportAddView") RapportView view, BindingResult binding) throws AdressesResolutionException {

		if (binding.hasErrors()) {
			return "tiers/edition/rapport/add";
		}

		final Long numeroTiers = view.getTiers().getNumero();
		final Long numeroTiersLie = view.getTiersLie().getNumero();

		final Tiers tiers = tiersDAO.get(numeroTiers);
		if (tiers == null) {
			throw new TiersNotFoundException(numeroTiers);
		}
		final Tiers tiersLie = tiersDAO.get(numeroTiersLie);
		if (tiersLie == null) {
			throw new TiersNotFoundException(numeroTiersLie);
		}

		// checks de sécurité
		controllerUtils.checkAccesDossierEnEcriture(numeroTiers);
		controllerUtils.checkAccesDossierEnEcriture(numeroTiersLie);
		if (!autorisationManager.isEditAllowed(tiers)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit d'édition des rapports-entre-tiers sur le tiers n°" + numeroTiers);
		}

		try {
			rapportEditManager.add(view);
		}
		catch (Exception e) {
			LOGGER.error("Erreur à l'ajout d'un rapport entre les tiers " + numeroTiers + " et " + numeroTiersLie, e);
			throw new ActionException(e.getMessage());
		}
		return URLHelper.navigateBackTo("/tiers/visu.do").defaultTo("/tiers/visu.do", "id=" + numeroTiers);
	}

	/**
	 * Affichage de l'écran d'édition d'un rapport-entre-tiers existant.
	 *
	 * @param idRapport l'id du rapport à éditer
	 * @param sens le sens du rapport qui permet d'ordonner le sujet et l'objet.
	 */
	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam("idRapport") long idRapport, @RequestParam("sens") SensRapportEntreTiers sens, Model model) throws AdresseException {

		final RapportEntreTiers rapport = rapportEntreTiersDAO.get(idRapport);
		if (rapport == null) {
			throw new ObjectNotFoundException("Le rapport-entre-tiers n°" + idRapport + " n'existe pas.");
		}

		final Long objetId = rapport.getObjetId();
		final Long sujetId = rapport.getSujetId();

		final Tiers sujet = tiersDAO.get(objetId);
		if (sujet == null) {
			throw new TiersNotFoundException(objetId);
		}
		final Tiers objet = tiersDAO.get(sujetId);
		if (objet == null) {
			throw new TiersNotFoundException(sujetId);
		}

		// checks de sécurité
		controllerUtils.checkAccesDossierEnLecture(objetId);
		controllerUtils.checkAccesDossierEnLecture(sujetId);
		if (!rapportEditManager.isEditionAllowed(idRapport, sens)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit d'édition du rapport-entre-tiers entre le tiers n°" + objetId + " et le tiers n°" + sujetId);
		}

		final RapportView rapportView = rapportEditManager.get(idRapport, sens);
		model.addAttribute("rapportEditView", rapportView);

		return "tiers/edition/rapport/edit";
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder(value = "rapportEditView")
	public void initBinderForEdit(WebDataBinder binder) {
		binder.addValidators(rapportEditValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Méthode de création d'un nouveau rapport-entre-tiers.
	 */
	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String edit(@ModelAttribute("rapportEditView") RapportView view, BindingResult binding) throws AdressesResolutionException {

		if (binding.hasErrors()) {
			return "tiers/edition/rapport/edit";
		}

		final Long numeroTiers = view.getNumeroCourant();
		final Long numeroTiersLie = view.getNumero();

		// checks de sécurité
		controllerUtils.checkAccesDossierEnEcriture(numeroTiers);
		controllerUtils.checkAccesDossierEnEcriture(numeroTiersLie);
		if (!rapportEditManager.isEditionAllowed(view.getId(), view.getSensRapportEntreTiers())) {
			throw new AccessDeniedException("Vous ne possédez pas le droit d'édition du rapport-entre-tiers entre le tiers n°" + numeroTiers + " et le tiers n°" + numeroTiersLie);
		}

		try {
			rapportEditManager.update(view);
		}
		catch (Exception e) {
			LOGGER.error("Erreur à la modification d'un rapport entre les tiers " + numeroTiers + " et " + numeroTiersLie, e);
			throw new ActionException(e.getMessage());
		}

		return URLHelper.navigateBack("/rapport/list.do", "id=" + view.getNumeroCourant());
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder(value = "principalView")
	public void initBinderForSetPrincipal(WebDataBinder binder) {
		binder.setValidator(setPrincipalValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Affiche le formulaire de sélection d'un héritier comme principal d'une communauté d'héritiers.
	 */
	@RequestMapping(value = "/setprincipal.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String setPrincipal(@RequestParam("idRapport") long idRapport, Model model) {

		final RapportEntreTiers rapport = rapportEntreTiersDAO.get(idRapport);
		if (rapport == null) {
			throw new ObjectNotFoundException("Le rapport-entre-tiers n°" + idRapport + " n'existe pas.");
		}
		if (!(rapport instanceof Heritage)) {
			throw new IllegalArgumentException("Le rapport-entre-tiers n°" + idRapport + " n'est pas un héritage.");
		}

		final Long defuntId = rapport.getObjetId();
		final Long heritierId = rapport.getSujetId();

		// checks de sécurité
		controllerUtils.checkAccesDossierEnLecture(defuntId);
		controllerUtils.checkAccesDossierEnLecture(heritierId);

		model.addAttribute("principalView", new SetPrincipalView(idRapport, defuntId, heritierId));
		model.addAttribute("principaux", getHistoPrincipaux(defuntId));

		return "tiers/edition/rapport/set-principal";
	}

	/**
	 * Sélectionne l'héritier spécifié comme principal de la communauté d'héritier.
	 */
	@RequestMapping(value = "/setprincipal.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String setPrincipal(@Valid @ModelAttribute("principalView") SetPrincipalView view, BindingResult binding, Model model) {

		if (binding.hasErrors()) {
			model.addAttribute("principaux", getHistoPrincipaux(view.getDefuntId()));
			return "tiers/edition/rapport/set-principal";
		}

		final Long defuntId = view.getDefuntId();
		final Long heritierId = view.getHeritierId();

		// checks de sécurité
		controllerUtils.checkAccesDossierEnEcriture(defuntId);
		controllerUtils.checkAccesDossierEnEcriture(heritierId);

		rapportEditManager.setPrincipal(view.getDefuntId(), view.getHeritierId(), view.getDateDebut());
		Flash.message("Le tiers n°" + FormatNumeroHelper.numeroCTBToDisplay(view.getHeritierId()) +
				              " a été désigné comme principal de la communauté à partir du " +
				              RegDateHelper.dateToDisplayString(view.getDateDebut()) + ".");

		return URLHelper.navigateBackTo("/rapport/list.do").defaultTo("/rapport/list.do", "id=" + view.getDefuntId());
	}

	/**
	 * @param defuntId l'id d'un tiers décédé
	 * @return l'historique des héritages avec le flag 'principal' actif
	 */
	private List<RapportView> getHistoPrincipaux(Long defuntId) {
		final Tiers defunt = tiersDAO.get(defuntId);
		if (defunt == null) {
			throw new TiersNotFoundException(defuntId);
		}
		return defunt.getRapportsObjet().stream()
				.filter(Heritage.class::isInstance)
				.map(Heritage.class::cast)
				.filter(h -> h.getPrincipalCommunaute() != null && h.getPrincipalCommunaute())
				.map(r -> new RapportView(r, SensRapportEntreTiers.OBJET, tiersService, adresseService))
				.collect(Collectors.toList());
	}

	/**
	 * Méthode d'annulation d'un rapport-entre-tiers.
	 */
	@RequestMapping(value = "/cancel.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String cancel(@RequestParam("id") long rapportId) throws AdressesResolutionException {

		final RapportEntreTiers rapport = rapportEntreTiersDAO.get(rapportId);
		if (rapport == null) {
			throw new ObjectNotFoundException("Le rapport n°" + rapportId + " n'existe pas.");
		}

		final Long sujetId = rapport.getSujetId();
		final Long objetId = rapport.getObjetId();

		final Tiers sujet = tiersDAO.get(sujetId);
		if (sujet == null) {
			throw new TiersNotFoundException(sujetId);
		}
		final Tiers objet = tiersDAO.get(objetId);
		if (objet == null) {
			throw new TiersNotFoundException(objetId);
		}

		// checks de sécurité
		controllerUtils.checkAccesDossierEnEcriture(sujetId);
		controllerUtils.checkAccesDossierEnEcriture(objetId);
		if (!autorisationManager.isEditAllowed(sujet)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit d'édition des rapports-entre-tiers sur le tiers n°" + sujetId);
		}
		if (!autorisationManager.isEditAllowed(objet)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit d'édition des rapports-entre-tiers sur le tiers n°" + objetId);
		}

		// annulation du rapport
		try {
			rapportEditManager.annulerRapport(rapportId);
		}
		catch (Exception e) {
			Flash.error("Impossible d'annuler le rapport n°" + rapportId + " pour la raison suivante: " + e.getMessage());
		}

		return URLHelper.navigateBackTo("/rapport/list.do").defaultTo("/rapport/list.do", "id=" + sujetId);
	}

	/**
	 * Retourne les rapports d'un contribuable page par page et sous format JSON.
	 *
	 * @param tiersId   le numéro de tiers
	 * @param showHisto <b>vrai</b> s'il faut afficher les valeurs historiques; <b>faux</b> autrement.
	 * @param type      le type de rapport à afficher, ou <b>null</b> s'il faut afficher tous les types de rapports
	 * @param sortField le champ sur lequel le tri doit être fait
	 * @param sortOrder le sens du tri ('ASC' ou 'DESC')
	 * @param page      le numéro de page à retourner
	 * @param pageSize  la taille des pages
	 * @return les informations nécessaire à l'affichage d'une page de rapports du contribuable.
	 * @throws ch.vd.unireg.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/rapports.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public RapportsPage rapports(@RequestParam("tiers") long tiersId,
	                             @RequestParam(value = "showHisto", required = false, defaultValue = "false") boolean showHisto,
	                             @RequestParam(value = "type", required = false) String type,
	                             @RequestParam(value = "sortField", required = false) String sortField,
	                             @RequestParam(value = "sortOrder", required = false) String sortOrder,
	                             @RequestParam(value = "page", required = false, defaultValue = "1") int page,
	                             @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les rapports entre tiers d'un contribuable");
		}

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final TypeRapportEntreTiers typeRapport = parseType(type);
		final Set<RapportEntreTiersKey> allowedKeys = getAllowedTypes();
		final Set<RapportEntreTiersKey> keys = buildTypeSet(typeRapport, allowedKeys);
		final int totalCount = getRapportsTotalCount(tiers, showHisto, keys);

		page = ParamPagination.adjustPage(page, pageSize, totalCount);
		final ParamPagination pagination = new ParamPagination(page, pageSize, sortField, "ASC".equalsIgnoreCase(sortOrder));

		final Map<TypeRapportEntreTiers, String> allTypes = tiersMapHelper.getMapTypeRapportEntreTiers();
		final Map<TypeRapportEntreTiers, String> choosableTypes = new LinkedHashMap<>(allTypes);
		final Set<TypeRapportEntreTiers> allowedTypes = EnumSet.noneOf(TypeRapportEntreTiers.class);
		for (RapportEntreTiersKey key : allowedKeys) {
			allowedTypes.add(key.getType());
		}
		choosableTypes.keySet().retainAll(allowedTypes);

		final List<RapportsPage.RapportView> views = getRapportViews(tiersId, showHisto, keys, pagination, false);
		return new RapportsPage(tiersId, views, showHisto, typeRapport, choosableTypes, page, totalCount, sortField, sortOrder);
	}

	/**
	 * Retourne les parentés d'un contribuable sous format JSON.
	 *
	 * @param tiersId   le numéro de tiers
	 * @return les informations nécessaire à l'affichage d'une page de parentés du contribuable.
	 * @throws ch.vd.unireg.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/parentes.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public RapportsPage parentes(@RequestParam("tiers") long tiersId) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les parentés d'un contribuable");
		}

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final int totalCount = getRapportsTotalCount(tiers, true, RapportHelper.ALLOWED_PARENTES);

		final ParamPagination pagination = new ParamPagination(1, Integer.MAX_VALUE, "tiersId", true);
		final List<RapportsPage.RapportView> views = getRapportViews(tiersId, true, RapportHelper.ALLOWED_PARENTES, pagination, true);
		return new RapportsPage(tiersId, views, true, TypeRapportEntreTiers.PARENTE, null, 1, totalCount, null, null);
	}

	/**
	 * Retourne toutes les débiteurs associés à un contribuable sous format JSON.
	 *
	 * @param tiersId le numéro de tiers
	 * @return une liste de liens vers les débiteurs associés sous format JSON
	 * @throws ch.vd.unireg.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/debiteurs.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public List<DebiteurView> debiteurs(@RequestParam("tiers") long tiersId) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les immeubles d'un contribuable");
		}

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		return getDebiteurViews(tiers);
	}

	/**
	 * Retourne toutes les établissements (principal et secondaire) associés à un contribuable sous format JSON.
	 *
	 * @param tiersId le numéro de tiers
	 * @return une liste de liens vers les établissements sous format JSON
	 * @throws ch.vd.unireg.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/etablissements.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public RapportsPage etablissements(@RequestParam("tiers") long tiersId,
	                             @RequestParam(value = "showHisto", required = false, defaultValue = "false") boolean showHisto,
	                             @RequestParam(value = "sortField", required = false) String sortField,
	                             @RequestParam(value = "sortOrder", required = false) String sortOrder,
	                             @RequestParam(value = "page", required = false, defaultValue = "1") int page,
	                             @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les établissements d'un tiers");
		}

		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final int totalCount = getRapportsTotalCount(tiers, showHisto, RapportHelper.ALLOWED_ETABLISSEMENTS);

		page = ParamPagination.adjustPage(page, pageSize, totalCount);
		final ParamPagination pagination = new ParamPagination(page, pageSize, ((StringUtils.isBlank(sortField)) ? null : sortField), "ASC".equalsIgnoreCase(sortOrder));

		// Si pas de tri défini, on met les annulés en queue de liste
		final List<RapportsPage.RapportView> views;
		if (StringUtils.isBlank(sortField)) {
			views = getRapportViews(tiersId, showHisto, RapportHelper.ALLOWED_ETABLISSEMENTS, pagination, true);
		}
		else {
			views = getRapportViews(tiersId, showHisto, RapportHelper.ALLOWED_ETABLISSEMENTS, pagination, false);
		}

		return new RapportsPage(tiersId, views, showHisto, null, null, page, totalCount, sortField, sortOrder);
	}

	private List<RapportsPage.RapportView> getRapportViews(long tiersId, boolean showHisto, Set<RapportEntreTiersKey> types, final ParamPagination pagination, boolean annulesEnDernier) {
		// Récupération de tous les rapports (triés)
		final List<RapportEntreTiers> rapports = rapportEntreTiersDAO.findBySujetAndObjet(tiersId, showHisto, types, pagination);

		// Tri
		final String sortingField = pagination.getSorting().getField();

		if (sortingField != null && "autoriteTutelaire".equals(sortingField)) {
			sortRapportsByAutoriteTutelaire(pagination, rapports, this.tiersService);
		}

		if (annulesEnDernier) {
			rapports.sort(new Comparator<RapportEntreTiers>() {
				@Override
				public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
					// on garde l'ordre fourni par la base en plaçant les éléments annulés à la fin
					if (o1.isAnnule() != o2.isAnnule()) {
						return o1.isAnnule() ? 1 : -1;
					}
					else {
						// on ne change pas l'ordre donné (car la méthode Collections#sort est dite "stable")
						return 0;
					}
				}
			});
		}

		// On ne peuple dans la vue que les rapports concernés par la page courante
		List<RapportEntreTiers> currentRapports = new ArrayList<>(rapports);
		if (currentRapports.size() > pagination.getSqlMaxResults()) {
			int toIndex = (pagination.getSqlMaxResults()*pagination.getNumeroPage() > rapports.size()) ? rapports.size() : pagination.getSqlMaxResults()*pagination.getNumeroPage();
			currentRapports = rapports.subList(pagination.getSqlFirstResult(), toIndex);
		}

		if (currentRapports.size() > 5) {
			prechargeIndividus(currentRapports);
		}

		final List<RapportsPage.RapportView> views = new ArrayList<>(currentRapports.size());
		for (RapportEntreTiers r : currentRapports) {
			if (r.getObjetId().equals(tiersId)) {
				views.add(new RapportsPage.RapportView(r, SensRapportEntreTiers.OBJET, tiersService, adresseService, messageSource));
			}
			else if (r.getSujetId().equals(tiersId)) {
				views.add(new RapportsPage.RapportView(r, SensRapportEntreTiers.SUJET, tiersService, adresseService, messageSource));
			}
		}
		return views;
	}

	/**
	 * [SIFISC-26747] Tri sur la colonne autorité tutélaire
	 *
	 * @param pagination
	 * @param rapports
	 */
	static void sortRapportsByAutoriteTutelaire(ParamPagination pagination, List<RapportEntreTiers> rapports, TiersService tiersService) {
		final Comparator<RapportEntreTiers> comparateurAsc;
		comparateurAsc = new Comparator<RapportEntreTiers>() {
			@Override
			public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
				String nomAutoriteTutelaire1 = "";
				String nomAutoriteTutelaire2 = "";
				// [SIFISC-26747] Tri par nom de l'autorité Tutélaire
				if (o1 instanceof RepresentationLegale) {
					final RepresentationLegale rl = (RepresentationLegale) o1;
					nomAutoriteTutelaire1 = RapportsPage.RapportView.getNomAutoriteTutelaire(rl.getAutoriteTutelaireId(), tiersService);
				}
				if (o2 instanceof RepresentationLegale) {
					final RepresentationLegale rl = (RepresentationLegale) o2;
					nomAutoriteTutelaire2 = RapportsPage.RapportView.getNomAutoriteTutelaire(rl.getAutoriteTutelaireId(), tiersService);
				}

				if (StringUtils.isBlank(nomAutoriteTutelaire1) || StringUtils.isBlank(nomAutoriteTutelaire2)) {
					if (StringUtils.isBlank(nomAutoriteTutelaire1) && StringUtils.isBlank(nomAutoriteTutelaire2)) {
						return 0;
					}

					return StringUtils.isBlank(nomAutoriteTutelaire1) ? -1 : 1;
				}

				return nomAutoriteTutelaire1.compareTo(nomAutoriteTutelaire2);
			}
		};

		rapports.sort(comparateurAsc);
		if (!pagination.getSorting().isAscending()) {
			Collections.reverse(rapports);
		}
	}

	private static TypeRapportEntreTiers parseType(String type) {
		if (StringUtils.isBlank(type)) {
			return null;
		}
		try {
			return TypeRapportEntreTiers.valueOf(type);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	private List<DebiteurView> getDebiteurViews(Tiers tiers) {

		if (!(tiers instanceof Contribuable)) {
			return null;
		}

		final List<DebiteurView> views = new ArrayList<>();

		final Contribuable contribuable = (Contribuable) tiers;
		final Set<RapportEntreTiers> rapports = contribuable.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers r : rapports) {
				if (r instanceof ContactImpotSource) {
					final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(r.getObjetId());
					final DebiteurView view = new DebiteurView(dpi, (ContactImpotSource) r, adresseService, messageSource);
					views.add(view);
				}
			}
		}

		return views;
	}

	private int getRapportsTotalCount(Tiers tiers, boolean showHisto, Set<RapportEntreTiersKey> types) {
		return rapportEntreTiersDAO.countBySujetAndObjet(tiers.getNumero(), showHisto, types);
	}

	private void prechargeIndividus(List<RapportEntreTiers> rapports) {
		if (cacheWarmer.isServiceWarmable()) {
			final Set<Long> tiersIds = new HashSet<>();
			for (RapportEntreTiers rapport : rapports) {
				tiersIds.add(rapport.getSujetId());
				tiersIds.add(rapport.getObjetId());
			}
			cacheWarmer.warmIndividusPourTiers(tiersIds, null, true, AttributeIndividu.ADRESSES);
		}
	}
}
