package ch.vd.uniregctb.tache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.EditiqueCommunicationException;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.tache.manager.TacheListManager;
import ch.vd.uniregctb.tache.validator.TachesValidator;
import ch.vd.uniregctb.tache.view.ImpressionNouveauxDossiersView;
import ch.vd.uniregctb.tache.view.NouveauDossierCriteriaView;
import ch.vd.uniregctb.tache.view.NouveauDossierListView;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;
import ch.vd.uniregctb.tache.view.TacheListView;
import ch.vd.uniregctb.type.TypeEtatTache;

@Controller
@RequestMapping("/tache")
public class TacheController {

	private static final Logger LOGGER = Logger.getLogger(TacheController.class);

	private static final String TACHE_CRITERIA_NAME = "tacheCriteria";
	private static final String NOUVEAU_DOSSIER_CRITERIA_NAME = "nouveauDossierCriteria";

	private static final String TACHE_PAGINATION_NAME = "tachePagination";

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
	 * Le nom de l'attribut utilise pour les periodes fiscales
	 */
	private static final String PERIODE_FISCALE_MAP_NAME = "periodesFiscales";


	private static final String RESULT_SIZE_NAME = "resultSize";
	private static final Integer PAGE_SIZE = 25;

	private static final String TACHE_LIST_ATTRIBUTE_NAME = "taches";
	private static final String TABLE_TACHE_ID = "tache";

	private static final String NOUVEAU_DOSSIER_LIST_ATTRIBUTE_NAME = "nouveauxDossiers";
	private static final String TABLE_NOUVEAU_DOSSIER_ID = "nouveauDossier";

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

	private Pair<WebParamPagination, Boolean> getTachePagination(HttpServletRequest request) {
		final WebParamPagination pagination;

		final String inRequest = controllerUtils.getDisplayTagRequestParametersForPagination(request, TABLE_TACHE_ID);
		boolean fromSession = false;
		if (inRequest != null) {
			pagination = new WebParamPagination(request, TABLE_TACHE_ID, PAGE_SIZE);
		}
		else {
			final WebParamPagination inSession = (WebParamPagination) request.getSession().getAttribute(TACHE_PAGINATION_NAME);
			fromSession = inSession != null;
			pagination = fromSession ? inSession : new WebParamPagination(1, PAGE_SIZE, "id", false);
		}
		request.getSession().setAttribute(TACHE_PAGINATION_NAME, pagination);
		return Pair.of(pagination, fromSession);
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String showTaches(Model model, HttpSession session, HttpServletRequest request, @RequestParam(value = "effacer", defaultValue = "false") boolean effacer) throws Exception {

		if (effacer) {
			session.removeAttribute(TACHE_CRITERIA_NAME);
			session.removeAttribute(TACHE_PAGINATION_NAME);
		}

		TacheCriteriaView view = (TacheCriteriaView) session.getAttribute(TACHE_CRITERIA_NAME);
		if (view == null) {
			view = new TacheCriteriaView();
			view.setEtatTache(TypeEtatTache.EN_INSTANCE);
			view.setOfficeImpot(getDefaultOID());
		}

		if (!effacer) {

			final Pair<WebParamPagination, Boolean> pagination = getTachePagination(request);
			if (pagination.getRight() && pagination.getLeft().getNumeroPage() != 1) {
				final String params = controllerUtils.getDisplayTagRequestParametersForPagination(TABLE_TACHE_ID, pagination.getLeft());
				return "redirect:/tache/list.do?" + params;
			}

			final List<TacheListView> tachesView = tacheListManager.find(view, pagination.getLeft());
			model.addAttribute(TACHE_LIST_ATTRIBUTE_NAME, tachesView);
			model.addAttribute(RESULT_SIZE_NAME, tacheListManager.count(view));
		}
		return showSearchTaches(model, view, effacer);
	}

	private String showSearchTaches(Model model, TacheCriteriaView criteria, boolean forceEmptyResult) {
		model.addAttribute("command", criteria);
		model.addAttribute(PERIODE_FISCALE_MAP_NAME, tacheMapHelper.initMapPeriodeFiscale());
		model.addAttribute(OFFICE_IMPOT_UTILISATEUR_MAP_NAME, tacheMapHelper.initMapOfficeImpotUtilisateur());
		model.addAttribute(ETAT_TACHE_MAP_NAME, tacheMapHelper.initMapEtatTache());
		model.addAttribute(TYPE_TACHE_MAP_NAME, tacheMapHelper.initMapTypeTache());
		if (forceEmptyResult) {
			model.addAttribute(TACHE_LIST_ATTRIBUTE_NAME, new ArrayList<TacheListView>());
			model.addAttribute(RESULT_SIZE_NAME, 0);
		}
		return "tache/list";
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	public String searchTaches(Model model, HttpSession session, @Valid @ModelAttribute TacheCriteriaView criteria, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showSearchTaches(model, criteria, true);
		}

		session.removeAttribute(TACHE_PAGINATION_NAME);
		session.setAttribute(TACHE_CRITERIA_NAME, criteria);
		return "redirect:/tache/list.do";
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
		model.addAttribute("command", criteria);
		model.addAttribute(OFFICE_IMPOT_UTILISATEUR_MAP_NAME, tacheMapHelper.initMapOfficeImpotUtilisateur());
		if (forceEmptyResult) {
			model.addAttribute(NOUVEAU_DOSSIER_LIST_ATTRIBUTE_NAME, new ArrayList<>());
			model.addAttribute(RESULT_SIZE_NAME, 0);
		}
		return "nouveau-dossier/list";
	}

	@RequestMapping(value = "/list-nouveau-dossier.do", method = RequestMethod.POST)
	public String searchNouveauxDossiers(Model model, HttpSession session, @Valid @ModelAttribute NouveauDossierCriteriaView criteria, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showSearchNouveauxDossiers(model, criteria, true);
		}

		session.setAttribute(NOUVEAU_DOSSIER_CRITERIA_NAME, criteria);
		return "redirect:/tache/list-nouveau-dossier.do";
	}

	@RequestMapping(value = "/imprimer-nouveaux-dossiers.do", method = RequestMethod.POST)
	public String printNouveauxDossiers(HttpServletResponse response, @ModelAttribute ImpressionNouveauxDossiersView view) throws Exception {
		final RetourEditiqueControllerHelper.TraitementRetourEditique erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique() {
			@Override
			public String doJob(EditiqueResultat resultat) {
				final String message = String.format("%s Veuillez recommencer l'opération ultérieurement.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
				throw new EditiqueCommunicationException(message);
			}
		};

		try {
			final EditiqueResultat res = tacheListManager.envoieImpressionLocalDossier(view);
			return editiqueControllerHelper.traiteRetourEditique(res, response, "dossier", null, erreur, erreur);
		}
		catch (EditiqueException e) {
			LOGGER.error(e, e);
			// UNIREG-1218 : on affiche le message d'erreur de manière sympa
			throw new ActionException(e.getMessage());
		}
	}

	/**
	 * @return le sigle de l'office d'impôt utilisé actuellement par l'utilisateur, ou <code>null</code> si le sigle ne peut être déterminé.
	 */
	protected static String getDefaultOID() {
		final Integer officeImpot = AuthenticationHelper.getCurrentOID();
		if (officeImpot == null) {
			return null;
		}
		return officeImpot.toString();
	}

}
