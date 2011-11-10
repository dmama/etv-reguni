package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.mouvement.manager.MouvementMasseManager;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaConsultationView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaSimpleEtatView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaTraitementView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseResultatRechercheView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/mouvement/masse")
public class MouvementMasseController {

	private static final String TRAITEMENT_FORM_SESSION_NAME = "MvtDossierMasseTraitementView";
	private static final String CONSULTATION_FORM_SESSION_NAME = "MvtDossierMasseConsultationView";

	private static final String ACTION = "action";
	private static final int PAGE_SIZE = 25;
	private static final String TABLE_ID = "mvt";

	private static final String ID = "id";
	private static final String PAGINATION = "pagination";

	private static final String MONTRER_INITIATEUR = "montrerInitiateur";
	private static final String MONTRER_EXPORT = "montrerExport";
	private static final String CRITERIA = "criteria";
	private static final String FOUND = "found";

	private MouvementMasseManager mouvementManager;

	private MouvementMapHelper mouvementMapHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementManager(MouvementMasseManager mouvementManager) {
		this.mouvementManager = mouvementManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementMapHelper(MouvementMapHelper mouvementMapHelper) {
		this.mouvementMapHelper = mouvementMapHelper;
	}

	private static String buildRedirectPourTraitement(@Nullable String pagination) {
		return buildRedirectWithPaginationParam("pour-traitement.do", pagination);
	}

	private static String buildRedirectPourConsultation(@Nullable String pagination) {
		return buildRedirectWithPaginationParam("consulter.do", pagination);
	}

	private static String buildRedirectWithPaginationParam(String action, @Nullable String pagination) {
		final String param;
		if (StringUtils.isNotBlank(pagination)) {
			param = String.format("?%s", StringUtils.trimToEmpty(pagination));
		}
		else {
			param = StringUtils.EMPTY;
		}
		return String.format("redirect:/mouvement/masse/%s%s", action, param);
	}

	private static WebParamPagination getParamPaginationFromRequest(HttpServletRequest request) {
		return new WebParamPagination(request, TABLE_ID, PAGE_SIZE, "contribuable.numero", true);
	}

	private static interface SearchCriteriaCustomizer {
		String getSessionAttributeName();
		MouvementMasseCriteriaView createNewEmptyCriteria();
		String getViewName();
		String getRedirectToPage();
	}

	private static final SearchCriteriaCustomizer SEARCH_CRITERIA_POUR_TRAITEMENT_CUSTOMIZER = new SearchCriteriaCustomizer() {
		@Override
		public String getSessionAttributeName() {
			return TRAITEMENT_FORM_SESSION_NAME;
		}

		@Override
		public MouvementMasseCriteriaView createNewEmptyCriteria() {
			return new MouvementMasseCriteriaTraitementView();
		}

		@Override
		public String getViewName() {
			return "mouvement/masse/traitement/list";
		}

		@Override
		public String getRedirectToPage() {
			return buildRedirectPourTraitement(null);
		}
	};

	private static final SearchCriteriaCustomizer SEARCH_CRITERIA_CONSULTATION_CUSTOMIZER = new SearchCriteriaCustomizer() {
		@Override
		public String getSessionAttributeName() {
			return CONSULTATION_FORM_SESSION_NAME;
		}

		@Override
		public MouvementMasseCriteriaView createNewEmptyCriteria() {
			return new MouvementMasseCriteriaSimpleEtatView();
		}

		@Override
		public String getViewName() {
			return "mouvement/masse/consult/list";
		}

		@Override
		public String getRedirectToPage() {
			return buildRedirectPourConsultation(null);
		}
	};

	private String getCriteresRecherche(HttpServletRequest request, Model model, @Nullable Action action, SearchCriteriaCustomizer customizer) {

		final String sessionAttributeName = customizer.getSessionAttributeName();
		final Integer noCollAdmInitiatrice = MouvementDossierHelper.getNoCollAdmFiltree();

		// on efface tout si on nous le demande
		final HttpSession session = request.getSession();
		if (action == Action.EFFACER) {
			session.setAttribute(sessionAttributeName, null);
		}

		final boolean montrerInitiateur = noCollAdmInitiatrice == null;

		final MouvementMasseCriteriaView criteria;
		final MouvementMasseResultatRechercheView found;
		final MouvementMasseCriteriaView inSessionCriteria = (MouvementMasseCriteriaView) session.getAttribute(sessionAttributeName);
		if (inSessionCriteria != null) {
			criteria = inSessionCriteria;
			found = doFind(request, criteria);

			if (action == Action.EXPORTER) {
				final WebParamPagination pagination = getParamPaginationFromRequest(request);
				final ExtractionJob job = mouvementManager.exportListeRecherchee(criteria, noCollAdmInitiatrice, pagination.getSorting());
				Flash.message(String.format("Demande d'export enregistrée (%s)", job.getDescription()));
			}

			// si la page demandée dans la pagination n'existe plus, on revient à la page 1 (= on supprime les infos de pagination)
			if (found != null && found.getResults().size() == 0 && found.getResultSize() > 0) {
				return customizer.getRedirectToPage();
			}
		}
		else {
			criteria = customizer.createNewEmptyCriteria();
			criteria.init();
			found = null;
		}

		model.addAttribute(CRITERIA, criteria);
		model.addAttribute(FOUND, found);
		model.addAttribute(MONTRER_EXPORT, found != null && found.getResultSize() > 0);
		model.addAttribute(MONTRER_INITIATEUR, montrerInitiateur);
		model.addAttribute(PAGINATION, ControllerUtils.getPaginatedTableParameters(request, TABLE_ID));
		return customizer.getViewName();
	}

	@InitBinder
	public void initBinder(HttpServletRequest request, WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false));

		final Locale locale = request.getLocale();
		final SimpleDateFormat sdf = new SimpleDateFormat(DateHelper.DATE_FORMAT_DISPLAY, locale);
		sdf.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
	}

	public static enum Action {
		EFFACER,
		EXPORTER
	}

	@RequestMapping(value = "/pour-traitement.do", method = RequestMethod.GET)
	public String getCriteresRecherchePourTraitement(HttpServletRequest request, Model model, @RequestParam(value = ACTION, required = false) Action action) {
		return getCriteresRecherche(request, model, action, SEARCH_CRITERIA_POUR_TRAITEMENT_CUSTOMIZER);
	}

	@RequestMapping(value = "/consulter.do", method = RequestMethod.GET)
	public String getCriteresPourConsultation(HttpServletRequest request, Model model, @RequestParam(value = ACTION, required = false) Action action) {
		mouvementMapHelper.putMapsIntoModel(model);
		return getCriteresRecherche(request, model, action, SEARCH_CRITERIA_CONSULTATION_CUSTOMIZER);
	}

	private MouvementMasseResultatRechercheView doFind(HttpServletRequest request, MouvementMasseCriteriaView view) throws ServiceInfrastructureException {
		final WebParamPagination pagination = getParamPaginationFromRequest(request);
		final MutableInt total = new MutableInt(0);
		final Integer noCollAdmInitiatrice = MouvementDossierHelper.getNoCollAdmFiltree();
		final List<MouvementDetailView> list = mouvementManager.find(view, noCollAdmInitiatrice, pagination, total);
		return new MouvementMasseResultatRechercheView(list, total.intValue());
	}

	@RequestMapping(value = "/rechercher-pour-traitement.do", method = RequestMethod.POST)
	public String rechercherPourTraitement(HttpSession session, @ModelAttribute(CRITERIA) MouvementMasseCriteriaTraitementView view) {
		session.setAttribute(TRAITEMENT_FORM_SESSION_NAME, view);
		return buildRedirectPourTraitement(null);
	}

	@RequestMapping(value = "rechercher-pour-consultation.do", method = RequestMethod.POST)
	public String rechercherPourConsultation(HttpSession session, @ModelAttribute(CRITERIA) MouvementMasseCriteriaConsultationView view) {
		session.setAttribute(CONSULTATION_FORM_SESSION_NAME, view);
		return buildRedirectPourConsultation(null);
	}

	@RequestMapping(value = "/reinit.do", method = RequestMethod.POST)
	public String reinitMouvementRetire(@RequestParam(value = ID, required = true) long idMvt,
	                                    @RequestParam(value = PAGINATION, required = false) String pagination) throws AccessDeniedException {

		MouvementDossierHelper.checkAccess();
		mouvementManager.changeEtat(EtatMouvementDossier.A_TRAITER, idMvt);
		return buildRedirectPourTraitement(pagination);
	}

	@RequestMapping(value = "/cancel.do", method = RequestMethod.POST)
	public String cancelMouvement(@RequestParam(value = ID, required = true) long idMvt,
	                              @RequestParam(value = PAGINATION, required = false) String pagination) throws AccessDeniedException {

		MouvementDossierHelper.checkAccess();
		mouvementManager.changeEtat(EtatMouvementDossier.RETIRE, idMvt);
		return buildRedirectPourTraitement(pagination);
	}

	@RequestMapping(value = "/inclure-dans-bordereau.do", method = RequestMethod.POST)
	public String inclureDansBordereau(@RequestParam(value = "tabIdsMvts") long[] idsMvts,
	                                   @RequestParam(value = PAGINATION, required = false) String pagination) throws AccessDeniedException {

		MouvementDossierHelper.checkAccess();
		mouvementManager.changeEtat(EtatMouvementDossier.A_ENVOYER, idsMvts);
		return buildRedirectPourTraitement(pagination);
	}
}
