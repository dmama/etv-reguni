package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.tiers.manager.SituationFamilleManager;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.utils.HttpSessionConstants;
import ch.vd.uniregctb.utils.HttpSessionUtils;

/**
 * @author xcifde
 *
 */
public class FiscalEditController extends AbstractTiersController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = LoggerFactory.getLogger(FiscalEditController.class);

	public static final String TARGET_ANNULER_SIT_FAM = "annulerSituationFamille";

	private TiersEditManager tiersEditManager;

	private ForFiscalManager forFiscalManager;

	private SituationFamilleManager situationFamilleManager;

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}

	public void setSituationFamilleManager(SituationFamilleManager situationFamilleManager) {
		this.situationFamilleManager = situationFamilleManager;
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		TiersEditView tiersView = new TiersEditView();
		String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);
		if (idParam != null) {
			Long id = Long.parseLong(idParam);
			if (!"".equals(idParam)) {
				//gestion des droits d'édition d'un tier par tiersEditManager
				checkAccesDossierEnLecture(id);
				tiersView = forFiscalManager.getView(id);

				@SuppressWarnings("ConstantConditions") final boolean forsPrincipauxPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.FORS_PRINCIPAUX_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.FORS_PRINCIPAUX_PAGINES));
				@SuppressWarnings("ConstantConditions") final boolean forsSecondairesPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.FORS_SECONDAIRES_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.FORS_SECONDAIRES_PAGINES));
				@SuppressWarnings("ConstantConditions") final boolean autresForsPrincipauxPagines = HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.AUTRES_FORS_PAGINES, Boolean.class, Boolean.TRUE, getOptionalBooleanParam(request, HttpSessionConstants.AUTRES_FORS_PAGINES));
				tiersView.setForsPrincipauxPagines(forsPrincipauxPagines);
				tiersView.setForsSecondairesPagines(forsSecondairesPagines);
				tiersView.setAutresForsPagines(autresForsPrincipauxPagines);
			}
		}
		return tiersView;
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		TiersEditView bean = (TiersEditView) command;
		checkAccesDossierEnEcriture(bean.getTiers().getId());

		if (getTarget() != null) {
			if (TARGET_ANNULER_SIT_FAM.equals(getTarget())) {
				String idSituationFamille = getEventArgument();
				if(idSituationFamille != null) {
					Long idSitFam = Long.parseLong(idSituationFamille);
					situationFamilleManager.annulerSituationFamille(idSitFam);
				}
			}
		} // button retour liste
		else if (request.getParameter(BUTTON_BACK_TO_LIST) != null) {
			return new ModelAndView("redirect:../tiers/list.do");
		} // button retour visualisation
		else if (request.getParameter(BUTTON_BACK_TO_VISU) != null) {
			return new ModelAndView("redirect:../tiers/visu.do?id=" + bean.getTiers().getNumero());
		}
		else if (request.getParameter(BUTTON_SAVE) != null) {
			this.setModified(false);
			Tiers tierSaved = tiersEditManager.save(bean);
			return new ModelAndView("redirect:../tiers/visu.do?id=" + tierSaved.getId());
		}

		tiersEditManager.refresh(bean,  bean.getTiers().getNumero());

		return showForm(request, response, errors);
	}

}
