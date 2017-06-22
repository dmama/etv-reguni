package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.utils.HttpSessionConstants;
import ch.vd.uniregctb.utils.HttpSessionUtils;

/**
 * @author xcifde
 *
 */
public class FiscalEditController extends AbstractTiersController {

	private TiersEditManager tiersEditManager;

	private ForFiscalManager forFiscalManager;

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
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

		if (request.getParameter(BUTTON_BACK_TO_LIST) != null) {
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
