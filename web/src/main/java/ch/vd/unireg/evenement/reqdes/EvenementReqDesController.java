package ch.vd.unireg.evenement.reqdes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.itextpdf.text.DocumentException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
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
import org.springframework.web.bind.annotation.SessionAttributes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.Fuse;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.evenement.reqdes.engine.EvenementReqDesProcessor;
import ch.vd.unireg.evenement.reqdes.pdf.UniteTraitementPdfDocumentGenerator;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.reqdes.EtatTraitement;
import ch.vd.unireg.reqdes.UniteTraitement;
import ch.vd.unireg.reqdes.UniteTraitementCriteria;
import ch.vd.unireg.reqdes.UniteTraitementDAO;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.servlet.ServletService;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/evenement/reqdes")
@SessionAttributes(value = {EvenementReqDesController.CRITERIA_NAME, EvenementReqDesController.PAGINATION_NAME})
public class EvenementReqDesController {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementReqDesController.class);

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec de gestion des événements des notaires.";
	private static final int RECYCLING_MAX_WAITING_TIME_MS = 3000;      // 3 secondes

	public static final String CRITERIA_NAME = "reqdesCriteria";
	public static final String PAGINATION_NAME = "reqdesPagination";

	private static final String TABLE_NAME = "tableUnitesTraitement";
	private static final String NB_UNITES_NAME = "nbUnites";
	private static final String LISTE_UNITES_NAME = "listUnites";

	private static final String ID = "id";
	private static final String UNITE_TRAITEMENT_NAME = "uniteTraitement";

	private static final int PAGE_SIZE = 25;
	private static final String DEFAULT_FIELD = ID;
	private static final WebParamPagination INITIAL_PAGINATION = new WebParamPagination(1, PAGE_SIZE, DEFAULT_FIELD, false);

	private ControllerUtils controllerUtils;
	private TiersMapHelper tiersMapHelper;
	private UniteTraitementDAO uniteTraitementDAO;
	private EvenementReqDesProcessor processor;
	private ServletService servletService;
	private ServiceInfrastructureService infraService;

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setUniteTraitementDAO(UniteTraitementDAO uniteTraitementDAO) {
		this.uniteTraitementDAO = uniteTraitementDAO;
	}

	public void setProcessor(EvenementReqDesProcessor processor) {
		this.processor = processor;
	}

	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@ModelAttribute
	public Model referenceData(Model model) throws Exception {
		model.addAttribute("etats", tiersMapHelper.getMapEtatsUniteTraitementReqDes());
		return model;
	}

	@ModelAttribute(value = CRITERIA_NAME)
	public ReqDesCriteriaView initCriteria() {
		final ReqDesCriteriaView criteria = new ReqDesCriteriaView();
		criteria.setEtat(EtatTraitement.EN_ERREUR);
		return criteria;
	}

	@ModelAttribute(value = PAGINATION_NAME)
	public WebParamPagination initPagination() {
		return INITIAL_PAGINATION;
	}

	@InitBinder(value = CRITERIA_NAME)
	protected final void initBinder(HttpServletRequest request, WebDataBinder binder) {
		binder.setValidator(new ReqDesCriteriaViewValidator());

		final Locale locale = request.getLocale();
		final NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(true);

		binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true));
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(Set.class, true));
		binder.registerCustomEditor(boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false));
	}

	private void populateSearchModel(Model model,
	                                 ReqDesCriteriaView criteres,
	                                 ParamPagination pagination,
	                                 List<ReqDesUniteTraitementListView> liste,
	                                 int totalSize) {
		model.addAttribute(CRITERIA_NAME, criteres);
		model.addAttribute(PAGINATION_NAME, pagination);
		model.addAttribute(LISTE_UNITES_NAME, liste);
		model.addAttribute(NB_UNITES_NAME, totalSize);
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String mainEntryPoint(@ModelAttribute(PAGINATION_NAME) ParamPagination pagination) {
		final String displayTagParameter = controllerUtils.getDisplayTagRequestParametersForPagination(TABLE_NAME, pagination);
		return String.format("redirect:/evenement/reqdes/nav-list.do%s%s",
		                     StringUtils.isNotBlank(displayTagParameter) ? "?" : StringUtils.EMPTY,
		                     StringUtils.trimToEmpty(displayTagParameter));
	}

	@RequestMapping(value = "/nav-list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String showList(HttpServletRequest request, Model model, @ModelAttribute(CRITERIA_NAME) @Valid ReqDesCriteriaView criteria, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			// on ré-affiche juste la page en effaçant les résultats des recherches précédentes et avec le message d'erreur
			populateSearchModel(model, criteria, INITIAL_PAGINATION, null, 0);
		}
		else {
			// récupération des paramètres de pagination
			final ParamPagination paramPagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, DEFAULT_FIELD, false);
			populateSearchModel(model, criteria, paramPagination, find(criteria, paramPagination), count(criteria));
		}
		return "evenement/reqdes/list";
	}

	@RequestMapping(value = "/effacer.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String effacerFormulaireDeRecherche(Model model) {
		ReqDesCriteriaView criteria = initCriteria();
		populateSearchModel(model, criteria, INITIAL_PAGINATION, find(criteria, INITIAL_PAGINATION), count(criteria));
		return "evenement/reqdes/list";
	}


	@RequestMapping(value = "/rechercher.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String doSearch(Model model, @ModelAttribute(CRITERIA_NAME) @Valid ReqDesCriteriaView criteria, BindingResult bindingResult) {
		populateSearchModel(model, criteria, INITIAL_PAGINATION, null, 0);
		if (bindingResult.hasErrors()) {
			// on ré-affiche juste la page en effaçant les résultats des recherches précédentes et avec le message d'erreur
			return "evenement/reqdes/list";
		}
		else {
			// retour à la première page pour les résultats correspondant à la nouvelle rercherhe
			return mainEntryPoint(INITIAL_PAGINATION);
		}
	}

	@RequestMapping(value = "/visu.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String showDetail(Model model, @RequestParam(ID) long idUniteTraitement) {
		model.addAttribute(UNITE_TRAITEMENT_NAME, get(idUniteTraitement));
		return "evenement/reqdes/visu";
	}

	@RequestMapping(value = "/recycler.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String recycler(@RequestParam(ID) final long idUniteTraitementARecycler) {

		final Fuse fusible = new Fuse();
		final EvenementReqDesProcessor.ListenerHandle handle = processor.registerListener(new EvenementReqDesProcessor.Listener() {
			@Override
			public void onUniteTraitee(long idUniteTraitement) {
				if (idUniteTraitement == idUniteTraitementARecycler) {
					fusible.blow();
				}
			}

			@Override
			public void onStop() {
				// rien à faire...
			}
		});
		try {
			processor.postUniteTraitement(idUniteTraitementARecycler);

			final long start = System.currentTimeMillis();
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (fusible) {
				// on n'attend quand-même pas trop longtemps...
				long now;
				while (fusible.isNotBlown() && (now = System.currentTimeMillis()) - start < RECYCLING_MAX_WAITING_TIME_MS) {
					fusible.wait(RECYCLING_MAX_WAITING_TIME_MS - now + start);
				}
			}
		}
		catch (InterruptedException e) {
			// rien de spécial à faire, on verra bien plus bas si le traitement a été fait ou pas...
		}
		finally {
			handle.unregister();
		}

		// traitement terminé ?
		if (fusible.isBlown()) {
			Flash.message("L'unité de traitement a été recyclée.");
		}
		else {
			Flash.warning("Le recyclage de l'unité de traitement a été demandé mais n'a pas encore pu être effectué.");
		}

		return String.format("redirect:/evenement/reqdes/visu.do?%s=%d", ID, idUniteTraitementARecycler);
	}

	@RequestMapping(value = "/forcer.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@Transactional(rollbackFor = Throwable.class)
	public String forcer(@RequestParam(ID) long idUniteTraitement) {
		final UniteTraitement ut = uniteTraitementDAO.get(idUniteTraitement);
		if (ut == null) {
			throw new ObjectNotFoundException("Pas d'unité de traitement avec l'identifiant " + idUniteTraitement);
		}
		if (ut.getEtat() == EtatTraitement.FORCE || ut.getEtat() == EtatTraitement.TRAITE) {
			Flash.message("L'unité de traitement est déjà dans un état final.");
		}

		LOGGER.info(String.format("L'unité de traitement ReqDes %d passe à l'état %s.", idUniteTraitement, EtatTraitement.FORCE));
		ut.setEtat(EtatTraitement.FORCE);

		return String.format("redirect:/evenement/reqdes/visu.do?%s=%d", ID, idUniteTraitement);
	}

	@RequestMapping(value = "/doc-ut.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String getDocUniteTraitement(@RequestParam(ID) long idUniteTraitement, HttpServletResponse response) {
		final UniteTraitement ut = uniteTraitementDAO.get(idUniteTraitement);
		if (ut == null) {
			throw new ObjectNotFoundException("Pas d'unité de traitement avec l'identifiant " + idUniteTraitement);
		}

		final UniteTraitementPdfDocumentGenerator document = new UniteTraitementPdfDocumentGenerator(ut);
		try {
			final byte[] content;
			try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				document.generatePdf(os, infraService);
				os.flush();
				content = os.toByteArray();
			}

			servletService.downloadAsFile("reqdes-" + idUniteTraitement + ".pdf", content, response);
		}
		catch (IOException | DocumentException e) {
			Flash.error("Une erreur est survenue : " + e.getMessage());
			LOGGER.error(e.getMessage(), e);
			return String.format("redirect:/evenement/reqdes/visu.do?%s=%d", ID, idUniteTraitement);
		}

		return null;
	}

	private static UniteTraitementCriteria buildCoreCriteria(ReqDesCriteriaView view) {
		final UniteTraitementCriteria core = new UniteTraitementCriteria();
		core.setNumeroMinute(StringUtils.trimToNull(view.getNumeroMinute()));
		core.setVisaNotaire(StringUtils.trimToNull(view.getVisaNotaire()));
		core.setDateTraitementMin(view.getDateTraitementMin());
		core.setDateTraitementMax(view.getDateTraitementMax());
		core.setEtatTraitement(view.getEtat());
		core.setDateReceptionMin(view.getDateReceptionMax());
		core.setDateTraitementMax(view.getDateReceptionMax());
		core.setDateActeMin(view.getDateActeMin());
		core.setDateActeMax(view.getDateActeMax());
		return core;
	}

	private List<ReqDesUniteTraitementListView> find(ReqDesCriteriaView criteria, ParamPagination pagination) {
		final List<UniteTraitement> uts = uniteTraitementDAO.find(buildCoreCriteria(criteria), pagination);
		final List<ReqDesUniteTraitementListView> views = new ArrayList<>(uts.size());
		for (UniteTraitement ut : uts) {
			views.add(new ReqDesUniteTraitementListView(ut));
		}
		return views;
	}

	private int count(ReqDesCriteriaView criteria) {
		return uniteTraitementDAO.getCount(buildCoreCriteria(criteria));
	}

	private ReqDesUniteTraitementDetailedView get(long idUniteTraitement) {
		final UniteTraitement ut = uniteTraitementDAO.get(idUniteTraitement);
		return ut == null ? null : new ReqDesUniteTraitementDetailedView(ut);
	}
}
