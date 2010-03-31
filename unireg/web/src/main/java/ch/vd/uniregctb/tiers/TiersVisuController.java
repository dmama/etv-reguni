package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.fidor.FidorService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tache.manager.TacheListManager;
import ch.vd.uniregctb.tiers.manager.TiersVisuManager;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.UniregProperties;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier
 * donne.
 *
 * @author XSIKCE</a>
 */
public class TiersVisuController extends AbstractTiersController {
	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(TiersVisuController.class);

	public final static String BUTTON_ANNULER_TIERS = "annulerTiers";

	private final static String ADRESSES_HISTO_PARAM = "adressesHisto";

	private final static String TACHE_ID_TRAITE_PARAM = "idTacheTraite";

	private TiersVisuManager tiersVisuManager ;

	private TacheListManager tacheListManager;

	private FidorService fidorService;

	public static final String PAGE_SIZE_NAME = "pageSize";
	public static final String RESULT_SIZE_NAME = "resultSize";
	private static final String TABLE_NAME = "rapportPrestation";
	private static final int PAGE_SIZE = 10;

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		TiersVisuView tiersVisuView = null;
		String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);
		String adrHistoParam = request.getParameter(ADRESSES_HISTO_PARAM);
		String idTacheTraiteParam = request.getParameter(TACHE_ID_TRAITE_PARAM);

		if (idParam != null && !idParam.equals("")) {
			Long id = Long.parseLong(idParam);

			// vérification des droits d'accès au dossier du contribuable
			checkAccesDossierEnLecture(id);

			WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE);
			if ((adrHistoParam == null) || (adrHistoParam.equals("false"))) {
				tiersVisuView = getTiersVisuManager().getView(id, false, pagination);
			}
			else {
				tiersVisuView = getTiersVisuManager().getView(id, true, pagination);
			}
			tiersVisuView.setUrlTaoPP(fidorService.getUrlTaoPP(id));
			tiersVisuView.setUrlTaoBA(fidorService.getUrlTaoBA(id));
			tiersVisuView.setUrlTaoIS(fidorService.getUrlTaoIS(id));
			tiersVisuView.setUrlSipf(fidorService.getUrlSipf(id));

			//vérification des droits de visualisation
			boolean isAllowed = true;
			if(tiersVisuView.getTiers() != null && !SecurityProvider.isGranted(Role.VISU_ALL)){
				if(!SecurityProvider.isGranted(Role.VISU_LIMITE)){
					throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
				}
				//pas de droits pour les inactifs, les DPI et les gris
				if(tiersVisuView.isDebiteurInactif() ||
					tiersVisuView.getNatureTiers().equals(Tiers.NATURE_DPI) ||
					(tiersVisuView.getNatureTiers().equals(Tiers.NATURE_NONHABITANT) &&
						tiersVisuView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)){
					isAllowed = false;
					tiersVisuView.setTiers(null);
				}
			}
			tiersVisuView.setAllowed(isAllowed);
		}
		if (idTacheTraiteParam != null && !idTacheTraiteParam.equals("")) {
			Long idTache = Long.parseLong(idTacheTraiteParam);
			tacheListManager.traiteTache(idTache);
		}
		return tiersVisuView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		HttpSession session = request.getSession();
		/*
		 * Ces warnings sont le résultat de la validation métier,
		 * pour cette raison ils sont stockés temporairement dans
		 * la session par le controlleur de séparation.
		 */
		List<String> warnings = (List<String>) session.getAttribute("warnings");
		mav.addObject("warnings", warnings);
		session.removeAttribute("warnings");

		mav.addObject(URL_RETOUR_SESSION_NAME, session.getAttribute(URL_RETOUR_SESSION_NAME));
		mav.addObject(PAGE_SIZE_NAME, PAGE_SIZE);
		String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);
		if (idParam != null && !idParam.equals("")) {
			Long numeroDebiteur = Long.parseLong(idParam);
			mav.addObject(RESULT_SIZE_NAME, tiersVisuManager.countRapportsPrestationImposable(numeroDebiteur));
		}
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		TiersVisuView bean = (TiersVisuView) command;
		checkAccesDossierEnEcriture(bean.getTiers().getId());

		if (request.getParameter(BUTTON_ANNULER_TIERS) != null) {
			tiersVisuManager.annulerTiers(bean.getTiers().getNumero());
			return new ModelAndView("redirect:visu.do?id=" + bean.getTiers().getNumero());
		}
		return showForm(request, response, errors);
	}

	public TiersVisuManager getTiersVisuManager() {
		return tiersVisuManager;
	}

	public void setTiersVisuManager(TiersVisuManager tiersVisuManager) {
		this.tiersVisuManager = tiersVisuManager;
	}

	public TacheListManager getTacheListManager() {
		return tacheListManager;
	}

	public void setTacheListManager(TacheListManager tacheListManager) {
		this.tacheListManager = tacheListManager;
	}

	public void setFidorService(FidorService fidorService) {
		this.fidorService = fidorService;
	}
}
