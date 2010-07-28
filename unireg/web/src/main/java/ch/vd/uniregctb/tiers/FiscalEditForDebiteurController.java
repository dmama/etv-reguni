package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;

public class FiscalEditForDebiteurController extends AbstractTiersController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(FiscalEditForDebiteurController.class);

	public final static String TARGET_ANNULER_FOR = "annulerFor";

	private TiersEditManager tiersEditManager;
	private ForFiscalManager forFiscalManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
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
			}
		}
		return tiersView;
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
			if (TARGET_ANNULER_FOR.equals(getTarget())) {
				String idForParam = getEventArgument();
				if (idForParam != null) {
					Long idFor = Long.parseLong(idForParam);
					forFiscalManager.annulerFor(idFor);
				}
			}
		} // button retour liste
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
