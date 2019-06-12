package ch.vd.unireg.tache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
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

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.EditiqueCommunicationException;
import ch.vd.unireg.common.EditiqueErrorHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.RetourEditiqueControllerHelper;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tache.manager.TacheListManager;
import ch.vd.unireg.tache.validator.TachesValidator;
import ch.vd.unireg.tache.view.ImpressionNouveauxDossiersView;
import ch.vd.unireg.tache.view.NouveauDossierCriteriaView;
import ch.vd.unireg.tache.view.NouveauDossierListView;
import ch.vd.unireg.tache.view.TacheCriteriaView;
import ch.vd.unireg.tache.view.TacheListView;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

@Controller
@RequestMapping("/tache")
public class TacheController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TacheController.class);

	private static final Integer PAGE_SIZE = 25;
	private static final String COMMAND_NAME = "command";
	private static final String NOUVEAU_DOSSIER_CRITERIA_NAME = "nouveauDossierCriteria";
	private static final String RESULT_SIZE_NAME = "resultSize";
	private static final String TACHE_LIST_NAME = "taches";
	private static final String TABLE_TACHE_ID = "tache";
	private static final String NOUVEAU_DOSSIER_LIST_ATTRIBUTE_NAME = "nouveauxDossiers";
	private static final String TABLE_NOUVEAU_DOSSIER_ID = "nouveauDossier";

	/**
	 * Nom de l'attribut qui conserve en session les critères de recherche utilisés pour les tâches
	 */
	private static final String TACHE_CRITERIA = "tacheCriteria";

	/**
	 * Nom de l'attribut qui conserve en session le tri et la pagination sur l'écran des tâches
	 */
	private static final String TACHE_PAGINATION = "tachePagination";

	/**
	 * Le nom de l'attribut utilise pour la liste des offices d'impôt de l'utilisateur
	 */
	private static final String OFFICE_IMPOT_UTILISATEUR_MAP_NAME = "officesImpotUtilisateur";

	/**
	 * Le nom de l'attribut utilise pour la liste des etats de tache
	 */
	private static final String ETAT_TACHE_MAP_NAME = "etatsTache";

	/**
	 * Le nom de l'attribut utilise pour la liste des etats de tache
	 */
	private static final String TYPE_TACHE_MAP_NAME = "typesTache";

	/**
	 * Le nom de l'attribut utilisé pour la liste des commentaires distincts dans les tâches de contrôle de dossier
	 */
	private static final String COMMENTAIRE_CTRL_MAP_NAME = "commentaires";

	/**
	 * Le nom de l'attribut utilise pour les periodes fiscales
	 */
	private static final String PERIODE_FISCALE_MAP_NAME = "periodesFiscales";

	private TacheMapHelper tacheMapHelper;
	private TacheListManager tacheListManager;
	private RetourEditiqueControllerHelper editiqueControllerHelper;
	private ControllerUtils controllerUtils;

	public void setTacheMapHelper(TacheMapHelper tacheMapHelper) {
		this.tacheMapHelper = tacheMapHelper;
	}

	public void setTacheListManager(TacheListManager tacheListManager) {
		this.tacheListManager = tacheListManager;
	}

	public void setEditiqueControllerHelper(RetourEditiqueControllerHelper editiqueControllerHelper) {
		this.editiqueControllerHelper = editiqueControllerHelper;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	@InitBinder
	public void initBinder(HttpServletRequest request, WebDataBinder binder) {
		final Locale locale = request.getLocale();
		final SimpleDateFormat sdf = new SimpleDateFormat(DateHelper.DATE_FORMAT_DISPLAY, locale);
		sdf.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
		binder.setValidator(new TachesValidator());
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String showTaches(@Valid @ModelAttribute(value = COMMAND_NAME) TacheCriteriaView criteria, BindingResult binding,
	                         @RequestParam(value = "effacer", required = false, defaultValue = "false") boolean effacer,
	                         @RequestParam(value = "realSearch", required = false, defaultValue = "false") boolean realSearch,
	                         Model model, HttpServletRequest request) throws Exception {

		initData(model);

		final HttpSession session = request.getSession();
		if (effacer) {
			session.removeAttribute(TACHE_CRITERIA);
			session.removeAttribute(TACHE_PAGINATION);
			return "redirect:list.do";
		}

		if (binding.hasErrors()) {
			model.addAttribute(TACHE_LIST_NAME, Collections.emptyList());
			model.addAttribute(RESULT_SIZE_NAME, 0);
			return "tache/list";
		}

		// retrouvons le bon critère
		final TacheCriteriaView actualCriteria;
		if (realSearch) {
			actualCriteria = criteria;
			session.setAttribute(TACHE_CRITERIA, criteria);
			session.removeAttribute(TACHE_PAGINATION);
		}
		else {
			final TacheCriteriaView inSessionCriteria = (TacheCriteriaView) session.getAttribute(TACHE_CRITERIA);
			actualCriteria = inSessionCriteria != null ? inSessionCriteria : new TacheCriteriaView();
		}
		model.addAttribute(COMMAND_NAME, actualCriteria);

		// nombre d'éléments trouvés
		final int count = tacheListManager.count(actualCriteria);
		final int numPageMax = Math.max(1, (count / PAGE_SIZE) + (count % PAGE_SIZE == 0 ? 0 : 1));

		// retrouvons le bon tri et la bonne pagination
		final WebParamPagination pagination;
		final String inRequestPaginationData = controllerUtils.getDisplayTagRequestParametersForPagination(request, TABLE_TACHE_ID);
		if (inRequestPaginationData != null) {
			// tri et pagination indiqués dans la requête, c'est ce que l'on utilise
			pagination = new WebParamPagination(request, TABLE_TACHE_ID, PAGE_SIZE);
		}
		else {
			// rien dans la requête... voyons voir dans la session
			final WebParamPagination inSessionPagination = (WebParamPagination) session.getAttribute(TACHE_PAGINATION);
			if (inSessionPagination != null) {
				if (inSessionPagination.getNumeroPage() > numPageMax) {
					// il y a moins de pages que la dernière fois, on dirait... on revient sur la dernière page
					pagination = new WebParamPagination(numPageMax, PAGE_SIZE, inSessionPagination.getChamp(), inSessionPagination.isSensAscending());
				}
				else {
					pagination = inSessionPagination;
				}

				// si le numéro de page est supérieur à un, il faut refaire passer ça dans la requête
				if (pagination.getNumeroPage() > 1) {
					return "redirect:list.do?" + controllerUtils.getDisplayTagRequestParametersForPagination(TABLE_TACHE_ID, pagination);
				}
			}
			else {
				// ok, page 1 avec éléments triés par ID descendant, alors...
				pagination = new WebParamPagination(1, PAGE_SIZE, "id", false);
			}
		}
		session.setAttribute(TACHE_PAGINATION, pagination);

		final List<TacheListView> tachesView = tacheListManager.find(actualCriteria, pagination);
		model.addAttribute(TACHE_LIST_NAME, tachesView);
		model.addAttribute(RESULT_SIZE_NAME, count);

		return "tache/list";
	}

	private void initData(Model model) {
		model.addAttribute(PERIODE_FISCALE_MAP_NAME, tacheMapHelper.initMapPeriodeFiscale());
		model.addAttribute(OFFICE_IMPOT_UTILISATEUR_MAP_NAME, initMapCollectivites());
		model.addAttribute(ETAT_TACHE_MAP_NAME, tacheMapHelper.initMapEtatTache());
		model.addAttribute(TYPE_TACHE_MAP_NAME, tacheMapHelper.initMapTypeTache());
		model.addAttribute(COMMENTAIRE_CTRL_MAP_NAME, tacheListManager.getCommentairesDistincts(TypeTache.TacheControleDossier));
	}

	private Map<Integer, String> initMapCollectivites() {
		final Integer oidConnexion = AuthenticationHelper.getCurrentOID();
		if (oidConnexion == null || oidConnexion == ServiceInfrastructureService.noACI) {
			return tacheMapHelper.initMapCollectivitesAvecTaches();
		}
		else {
			return tacheMapHelper.initMapOfficeImpotUtilisateur();
		}
	}

	@RequestMapping(value = "/list-nouveau-dossier.do", method = RequestMethod.GET)
	public String showNouveauxDossiers(Model model, HttpSession session, HttpServletRequest request, @RequestParam(value = "effacer", defaultValue = "false") boolean effacer) throws Exception {
		final NouveauDossierCriteriaView view = getNouveauDossierCriteriaView(session, effacer);
		if (!effacer) {
			final WebParamPagination pagination = new WebParamPagination(request, TABLE_NOUVEAU_DOSSIER_ID, PAGE_SIZE);
			final List<NouveauDossierListView> dossiersView = tacheListManager.find(view, pagination);
			model.addAttribute(NOUVEAU_DOSSIER_LIST_ATTRIBUTE_NAME, dossiersView);
			model.addAttribute(RESULT_SIZE_NAME, tacheListManager.count(view));
		}
		return showSearchNouveauxDossiers(model, view, effacer);
	}

	private NouveauDossierCriteriaView getNouveauDossierCriteriaView(HttpSession session, boolean effacer) {
		NouveauDossierCriteriaView view = (NouveauDossierCriteriaView) session.getAttribute(NOUVEAU_DOSSIER_CRITERIA_NAME);
		if (view == null || effacer) {
			session.removeAttribute(NOUVEAU_DOSSIER_CRITERIA_NAME);
			view = new NouveauDossierCriteriaView();
			view.setEtatTache(TypeEtatTache.EN_INSTANCE);
			view.setOfficeImpot(getDefaultOID());
		}
		return view;
	}

	private String showSearchNouveauxDossiers(Model model, NouveauDossierCriteriaView criteria, boolean forceEmptyResult) {
		model.addAttribute(COMMAND_NAME, criteria);
		model.addAttribute(OFFICE_IMPOT_UTILISATEUR_MAP_NAME, tacheMapHelper.initMapOfficeImpotUtilisateur());
		if (forceEmptyResult) {
			model.addAttribute(NOUVEAU_DOSSIER_LIST_ATTRIBUTE_NAME, new ArrayList<>());
			model.addAttribute(RESULT_SIZE_NAME, 0);
		}
		return "nouveau-dossier/list";
	}

	@RequestMapping(value = "/list-nouveau-dossier.do", method = RequestMethod.POST)
	public String searchNouveauxDossiers(Model model, HttpSession session, @Valid @ModelAttribute(COMMAND_NAME) NouveauDossierCriteriaView criteria, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showSearchNouveauxDossiers(model, criteria, true);
		}

		session.setAttribute(NOUVEAU_DOSSIER_CRITERIA_NAME, criteria);
		return "redirect:/tache/list-nouveau-dossier.do";
	}

	@RequestMapping(value = "/imprimer-nouveaux-dossiers.do", method = RequestMethod.POST)
	public String printNouveauxDossiers(HttpServletResponse response, @ModelAttribute ImpressionNouveauxDossiersView view) throws Exception {
		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = resultat -> {
			final String message = String.format("%s Veuillez recommencer l'opération ultérieurement.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
			throw new EditiqueCommunicationException(message);
		};

		try {
			final EditiqueResultat res = tacheListManager.envoieImpressionLocalDossier(view);
			return editiqueControllerHelper.traiteRetourEditique(res, response, "dossier", null, null, erreur);
		}
		catch (EditiqueException e) {
			LOGGER.error(e.getMessage(), e);
			// UNIREG-1218 : on affiche le message d'erreur de manière sympa
			throw new ActionException(e.getMessage());
		}
	}

	/**
	 * Méthode pour effectuer le contrôle d'un dossier.
	 */
	@RequestMapping(value = "/controller-dossier.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class)
	public String controllerDossier(@RequestParam(value = "tacheId") long tacheId) throws Exception {

		final Contribuable contribuable = tacheListManager.getContribuableFromTache(tacheId);
		if (contribuable == null) {
			throw new ObjectNotFoundException("Le contribuable de la tache n°" + tacheId + " est inconnu.");
		}

		// on passe la tâche comme traitée (bizarre, mais c'est le comportement historique)
		controllerUtils.checkAccesDossierEnEcriture(contribuable.getId());
		tacheListManager.traiteTache(tacheId);

		// on redirige vers la visualisation du contribuable pour qu'il contrôle le dossier
		return "redirect:/tiers/visu.do?id=" + contribuable.getId();
	}

	/**
	 * @return le sigle de l'office d'impôt utilisé actuellement par l'utilisateur, ou <code>null</code> si le sigle ne peut être déterminé (ou si cet office d'impôt est l'ACI).
	 */
	protected static String getDefaultOID() {
		final Integer officeImpot = AuthenticationHelper.getCurrentOID();
		if (officeImpot == null || officeImpot == ServiceInfrastructureService.noACI) {
			return null;
		}
		return officeImpot.toString();
	}

}
