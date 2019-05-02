package ch.vd.unireg.tiers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.TiersVisuManager;
import ch.vd.unireg.tiers.view.TiersVisuView;
import ch.vd.unireg.utils.HttpSessionUtils;
import ch.vd.unireg.utils.RegDateEditor;

import static ch.vd.unireg.utils.HttpSessionConstants.AUTRES_FORS_PAGINES;
import static ch.vd.unireg.utils.HttpSessionConstants.FORS_PRINCIPAUX_PAGINES;
import static ch.vd.unireg.utils.HttpSessionConstants.FORS_SECONDAIRES_PAGINES;
import static ch.vd.unireg.utils.HttpSessionConstants.URL_RETOUR_SESSION_NAME;
import static ch.vd.unireg.utils.HttpSessionUtils.getBooleanParam;
import static ch.vd.unireg.utils.HttpSessionUtils.getOptionalBooleanParam;

/**
 * Contrôleur du visualisation d'un tiers.
 */
@Controller
public class TiersVisuController {

	//private static final Logger LOGGER = LoggerFactory.getLogger(TiersVisuController.class);

	private static final String MODE_IMPRESSION = "printview";

	private TiersVisuManager tiersVisuManager;
	private AutorisationManager autorisationManager;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;

	public static final String PAGE_SIZE_NAME = "pageSize";
	public static final String RESULT_SIZE_NAME = "resultSize";
	private static final String TABLE_NAME = "rapportPrestation";
	private static final int PAGE_SIZE = 10;

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Affiche les informations d'un tiers.
	 *
	 * @param id l'id du tiers à afficher
	 */
	@RequestMapping(value = "/tiers/visu.do", method = RequestMethod.GET)
	@SuppressWarnings("ConstantConditions")
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String visu(@RequestParam(value = "id") long id, Model model, HttpServletRequest request) throws AdresseException, DonneesCivilesException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_LIMITE, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez pas les droits de consultation pour l'application Unireg");
		}

		// vérification des droits d'accès au dossier du contribuable
		controllerUtils.checkAccesDossierEnLecture(id);

		// interprétation des paramètres optionnels et de session
		final HistoFlags histoFlags = new HistoFlags(request);
		final boolean modeImpression = getBooleanParam(request, MODE_IMPRESSION);
		final boolean forsPrincipauxPagines = HttpSessionUtils.getFromSession(request.getSession(), FORS_PRINCIPAUX_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, FORS_PRINCIPAUX_PAGINES));
		final boolean forsSecondairesPagines = HttpSessionUtils.getFromSession(request.getSession(), FORS_SECONDAIRES_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, FORS_SECONDAIRES_PAGINES));
		final boolean autresForsPrincipauxPagines = HttpSessionUtils.getFromSession(request.getSession(), AUTRES_FORS_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, AUTRES_FORS_PAGINES));
		final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE);
		final String urlRetour = (String) request.getSession().getAttribute(URL_RETOUR_SESSION_NAME);

		// chargement des données du tiers
		final TiersVisuView tiersVisuView = tiersVisuManager.getView(id, histoFlags, modeImpression, forsPrincipauxPagines, forsSecondairesPagines, autresForsPrincipauxPagines, pagination);

		// vérification des droits fins de visualisation
		if (!autorisationManager.isVisuAllowed(tiersVisuView.getTiers())) {
			tiersVisuView.setAllowed(false);
			tiersVisuView.setTiers(null);
		}
		else {
			tiersVisuView.setAllowed(true);
		}

		// truc débile pour les DPIs (voir l'utilisation de 'resultSize' dans visu.jsp -> rapports-prestation.jsp -> common/rapports-prestation.jsp)
		final int rapportsPrestationCount;
		if (tiersVisuView.getTiers() instanceof DebiteurPrestationImposable) {
			rapportsPrestationCount = tiersVisuManager.countRapportsPrestationImposable(id, histoFlags.hasHistoFlag(HistoFlag.RAPPORTS_PRESTATION));
		}
		else {
			rapportsPrestationCount = 0;
		}

		model.addAttribute(URL_RETOUR_SESSION_NAME, urlRetour);
		model.addAttribute(PAGE_SIZE_NAME, PAGE_SIZE);
		model.addAttribute(RESULT_SIZE_NAME, rapportsPrestationCount);
		model.addAttribute("command", tiersVisuView);

		return "tiers/visualisation/visu";
	}

	public void setTiersVisuManager(TiersVisuManager tiersVisuManager) {
		this.tiersVisuManager = tiersVisuManager;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
