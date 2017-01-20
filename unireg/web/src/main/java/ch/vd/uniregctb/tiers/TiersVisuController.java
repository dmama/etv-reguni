package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.tache.manager.TacheListManager;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.manager.TiersVisuManager;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.HttpSessionConstants;
import ch.vd.uniregctb.utils.HttpSessionUtils;

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
	private static final String ADRESSES_HISTO_PARAM = "adressesHisto";
	private static final String ADRESSES_CIVILES_HISTO_PARAM = "adressesHistoCiviles";
	private static final String ADRESSES_CIVILES_HISTO_CONJ_PARAM = "adressesHistoCivilesConjoint";
	private static final String RAISONS_SOCIALES_HISTO_PARAM = "raisonsSocialesHisto";
	private static final String NOMS_ADDITIONNELS_HISTO_PARAM = "nomsAdditionnelsHisto";
	private static final String SIEGES_HISTO_PARAM = "siegesHisto";
	private static final String FORMES_JURIDIQUES_HISTO_PARAM = "formesJuridiquesHisto";
	private static final String CAPITAUX_HISTO_PARAM = "capitauxHisto";
	private static final String DOMICILES_HISTO_PARAM = "domicilesHisto";
	private static final String TACHE_ID_TRAITE_PARAM = "idTacheTraite";
	private static final String RAPPORTS_PREST_HISTO_PARAM = "rapportsPrestationHisto";
	private static final String CTB_ASSOCIE_HISTO_PARAM = "ctbAssocieHisto";
	private static final String MODE_IMPRESSION = "printview";

	private TiersEditManager tiersEditManager;
	private TiersVisuManager tiersVisuManager;
	private TacheListManager tacheListManager;

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
		final boolean adrHistoParam = getBooleanParam(request, ADRESSES_HISTO_PARAM);
		final boolean adrCivileHistoParam = getBooleanParam(request, ADRESSES_CIVILES_HISTO_PARAM);
		final boolean adrCivileHistoConjParam = getBooleanParam(request, ADRESSES_CIVILES_HISTO_CONJ_PARAM);
		final boolean raisonsSocialesHistoParam = getBooleanParam(request, RAISONS_SOCIALES_HISTO_PARAM);
		final boolean nomsAdditionnelsHistoParam = getBooleanParam(request, NOMS_ADDITIONNELS_HISTO_PARAM);
		final boolean siegesHistoParam = getBooleanParam(request, SIEGES_HISTO_PARAM);
		final boolean formesJuridiquesHistoParam = getBooleanParam(request, FORMES_JURIDIQUES_HISTO_PARAM);
		final boolean capitauxHistoParam = getBooleanParam(request, CAPITAUX_HISTO_PARAM);
		final boolean domicilesHistoParam = getBooleanParam(request, DOMICILES_HISTO_PARAM);
		final String idTacheTraiteParam = request.getParameter(TACHE_ID_TRAITE_PARAM);
		final boolean rapportsPrestationHisto = getBooleanParam(request, RAPPORTS_PREST_HISTO_PARAM);
		final boolean ctbAssocieHisto = getBooleanParam(request, CTB_ASSOCIE_HISTO_PARAM);
		final boolean modeImpression = getBooleanParam(request, MODE_IMPRESSION);

		@SuppressWarnings("ConstantConditions") final boolean forsPrincipauxPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.FORS_PRINCIPAUX_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.FORS_PRINCIPAUX_PAGINES));
		@SuppressWarnings("ConstantConditions") final boolean forsSecondairesPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.FORS_SECONDAIRES_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.FORS_SECONDAIRES_PAGINES));
		@SuppressWarnings("ConstantConditions") final boolean autresForsPrincipauxPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.AUTRES_FORS_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.AUTRES_FORS_PAGINES));

		if (idParam != null && !idParam.isEmpty()) {
			Long id = Long.parseLong(idParam);

			// vérification des droits d'accès au dossier du contribuable
			checkAccesDossierEnLecture(id);

			final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE);
			tiersVisuView = tiersVisuManager.getView(id, adrHistoParam, adrCivileHistoParam, adrCivileHistoConjParam,
			                                         raisonsSocialesHistoParam, nomsAdditionnelsHistoParam, siegesHistoParam, formesJuridiquesHistoParam, capitauxHistoParam, domicilesHistoParam,
			                                         rapportsPrestationHisto, ctbAssocieHisto,
			                                         modeImpression, forsPrincipauxPagines, forsSecondairesPagines, autresForsPrincipauxPagines, pagination);

			//vérification des droits de visualisation
			boolean isAllowed = true;
			if(tiersVisuView.getTiers() != null && !SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)){
				if(!SecurityHelper.isGranted(securityProvider, Role.VISU_LIMITE)){
					throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
				}
				//pas de droits pour les inactifs, les DPI et les gris
				if(tiersVisuView.isDebiteurInactif() ||
						tiersVisuView.getNatureTiers() == NatureTiers.DebiteurPrestationImposable ||
					(tiersVisuView.getNatureTiers() == NatureTiers.NonHabitant &&
						tiersVisuView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)){
					isAllowed = false;
					tiersVisuView.setTiers(null);
				}
			}
			tiersVisuView.setAllowed(isAllowed);
		}
		if (idTacheTraiteParam != null && !idTacheTraiteParam.isEmpty()) {
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

		final boolean rapportsPrestationHisto = getBooleanParam(request, RAPPORTS_PREST_HISTO_PARAM);

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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersVisuManager(TiersVisuManager tiersVisuManager) {
		this.tiersVisuManager = tiersVisuManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheListManager(TacheListManager tacheListManager) {
		this.tacheListManager = tacheListManager;
	}
}
