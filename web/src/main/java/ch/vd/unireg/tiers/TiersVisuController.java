package ch.vd.unireg.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.tiers.manager.TiersEditManager;
import ch.vd.unireg.tiers.manager.TiersVisuManager;
import ch.vd.unireg.tiers.view.TiersVisuView;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.utils.HttpSessionConstants;
import ch.vd.unireg.utils.HttpSessionUtils;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier
 * donne.
 *
 * @author XSIKCE
 */
public class TiersVisuController extends AbstractTiersController {
	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = LoggerFactory.getLogger(TiersVisuController.class);

	public static final String BUTTON_ANNULER_TIERS = "annulerTiers";

	private static final String MODE_IMPRESSION = "printview";

	private TiersEditManager tiersEditManager;
	private TiersVisuManager tiersVisuManager;

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

		final String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);

		final HistoFlags histoFlags = new HistoFlags(request);
		final boolean modeImpression = getBooleanParam(request, MODE_IMPRESSION);

		@SuppressWarnings("ConstantConditions") final boolean forsPrincipauxPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.FORS_PRINCIPAUX_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.FORS_PRINCIPAUX_PAGINES));
		@SuppressWarnings("ConstantConditions") final boolean forsSecondairesPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.FORS_SECONDAIRES_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.FORS_SECONDAIRES_PAGINES));
		@SuppressWarnings("ConstantConditions") final boolean autresForsPrincipauxPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.AUTRES_FORS_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.AUTRES_FORS_PAGINES));

		if (idParam != null && !idParam.isEmpty()) {
			Long id = Long.parseLong(idParam);

			// vérification des droits d'accès au dossier du contribuable
			checkAccesDossierEnLecture(id);

			final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE);
			tiersVisuView = tiersVisuManager.getView(id, histoFlags, modeImpression, forsPrincipauxPagines, forsSecondairesPagines, autresForsPrincipauxPagines, pagination);

			//vérification des droits de visualisation
			boolean isAllowed = true;
			if(tiersVisuView.getTiers() != null && !SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)){
				if(!SecurityHelper.isGranted(securityProvider, Role.VISU_LIMITE)){
					throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
				}
				// pas de droits pour les inactifs, les DPI et les gris (selon SIFISC-25963, gris = non-habitant avec for vaudois source-pure)
				if (tiersVisuView.isDebiteurInactif() ||
						tiersVisuView.getNatureTiers() == NatureTiers.DebiteurPrestationImposable ||
						(tiersVisuView.getNatureTiers() == NatureTiers.NonHabitant &&
								tiersVisuView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
								tiersVisuView.getModeImposition() == ModeImposition.SOURCE)) {
					isAllowed = false;
					tiersVisuView.setTiers(null);
				}
			}
			tiersVisuView.setAllowed(isAllowed);
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

		final HistoFlags histoFlags = new HistoFlags(request);
		final boolean rapportsPrestationHisto = histoFlags.hasHistoFlag(HistoFlag.RAPPORTS_PRESTATION);

		mav.addObject(URL_RETOUR_SESSION_NAME, session.getAttribute(URL_RETOUR_SESSION_NAME));
		mav.addObject(PAGE_SIZE_NAME, PAGE_SIZE);
		String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);
		if (idParam != null && !idParam.isEmpty()) {
			Long numeroDebiteur = Long.parseLong(idParam);
			mav.addObject(RESULT_SIZE_NAME, tiersVisuManager.countRapportsPrestationImposable(numeroDebiteur, rapportsPrestationHisto));
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
			tiersEditManager.annulerTiers(bean.getTiers().getNumero());
			return new ModelAndView("redirect:visu.do?id=" + bean.getTiers().getNumero());
		}
		return showForm(request, response, errors);
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	public void setTiersVisuManager(TiersVisuManager tiersVisuManager) {
		this.tiersVisuManager = tiersVisuManager;
	}
}
