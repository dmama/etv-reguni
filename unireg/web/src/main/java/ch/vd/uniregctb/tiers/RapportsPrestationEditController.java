package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.manager.RapportEditManager;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * @author xcifde
 *
 */
public class RapportsPrestationEditController extends AbstractTiersController {
	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(DossiersApparentesEditController.class);

	public final static String TARGET_ANNULER_RAPPORT = "annulerRapport";

	public static final String PAGE_SIZE_NAME = "pageSize";
	public static final String RESULT_SIZE_NAME = "resultSize";
	private static final String TABLE_NAME = "rapportPrestation";
	private static final int PAGE_SIZE = 10;

	private TiersEditManager tiersEditManager;
	private RapportEditManager rapportEditManager;
	public TiersEditManager getTiersEditManager() {
		return tiersEditManager;
	}
	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}
	public RapportEditManager getRapportEditManager() {
		return rapportEditManager;
	}
	public void setRapportEditManager(RapportEditManager rapportEditManager) {
		this.rapportEditManager = rapportEditManager;
	}
	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		TiersEditView tiersView = new TiersEditView();

		Long id = getLongParam(request, TIERS_ID_PARAMETER_NAME);

		if (id == null && getTarget() != null) {
			// si on va annuler un rapport, on va chercher l'id du tiers lié
			final Long idRapport = getLongEventArgument();
			final RapportView rapport = rapportEditManager.get(idRapport, SensRapportEntreTiers.SUJET); // on récupère l'id du débiteur
			id = rapport.getNumero();
		}

		if (id != null) {
			//gestion des droits d'édition d'un tiers par tiersEditManager
			checkAccesDossierEnLecture(id);
			WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE);
			tiersView = rapportEditManager.getRapportsPrestationView(id, pagination, true);
		}

		return tiersView;
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

		mav.addObject(PAGE_SIZE_NAME, PAGE_SIZE);
		String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);
		if (idParam != null && !idParam.isEmpty()) {
			Long numeroDebiteur = Long.parseLong(idParam);
			mav.addObject(RESULT_SIZE_NAME, getTiersEditManager().countRapportsPrestationImposable(numeroDebiteur, true));
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
		TiersEditView bean = (TiersEditView) command;
		checkAccesDossierEnEcriture(bean.getTiers().getId());

		if (getTarget() != null) {
			final boolean annulerRapport = TARGET_ANNULER_RAPPORT.equals(getTarget());
			if (annulerRapport) {
				final Long idRapport = getLongEventArgument();
				if (idRapport != null) {
					rapportEditManager.annulerRapport(idRapport);

					final String urlRetour = getUrlRetour();
					if (StringUtils.isNotBlank(urlRetour)) {
						return new ModelAndView("redirect:" + urlRetour);
					}
					else {
						return new ModelAndView("redirect:edit.do?id=" + bean.getTiers().getNumero());
					}
				}
			}
		}
		else if (request.getParameter(BUTTON_BACK_TO_LIST) != null) {
			return new ModelAndView("redirect:../tiers/list.do");
		} // button retour visualisation
		else if (request.getParameter(BUTTON_BACK_TO_VISU) != null) {
			return new ModelAndView("redirect:../tiers/visu.do?id=" + bean.getTiers().getNumero());
		}
		tiersEditManager.refresh(bean,  bean.getTiers().getNumero());

		return showForm(request, response, errors);
	}
}
