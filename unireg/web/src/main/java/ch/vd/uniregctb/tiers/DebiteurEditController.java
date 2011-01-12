package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;

public class DebiteurEditController extends AbstractTiersController {

	protected final Logger LOGGER = Logger.getLogger(DebiteurEditController.class);

	private TiersEditManager tiersEditManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		final DebiteurEditView view;
		final String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);
		if (StringUtils.isNotBlank(idParam)) {
			final Long id = Long.parseLong(idParam);
			//gestion des droits d'édition d'un tiers par tiersEditManager
			checkAccesDossierEnLecture(id);

			view = tiersEditManager.getDebiteurEditView(id);
		}
		else {
			view = new DebiteurEditView();
		}
		return view;
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final DebiteurEditView bean = (DebiteurEditView) command;
		checkAccesDossierEnEcriture(bean.getId());

		if (request.getParameter(BUTTON_SAVE) != null) {
			setModified(false);
			tiersEditManager.save(bean);
			return new ModelAndView("redirect:../tiers/visu.do?id=" + bean.getId());
		}
		else if (request.getParameter(BUTTON_BACK_TO_LIST) != null) {
			return new ModelAndView("redirect:../tiers/list.do");
		} // button retour visualisation
		else if (request.getParameter(BUTTON_BACK_TO_VISU) != null) {
			return new ModelAndView("redirect:../tiers/visu.do?id=" + bean.getId());
		}

		return showForm(request, response, errors);
	}

}
