package ch.vd.uniregctb.tiers;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.utils.RegDateEditor;

public class CivilEditController extends AbstractTiersController {

	//protected final Logger LOGGER = Logger.getLogger(CivilEditController.class);

	public final static String BUTTON_SAVE = "__confirmed_save";

	private TiersEditManager tiersEditManager;

	public TiersEditManager getTiersEditManager() {
		return tiersEditManager;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
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
				tiersView = tiersEditManager.getComplementView(id);
			}
		}
		return tiersView;
	}

	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
		super.initBinder(request, binder);

		// on doit autoriser les dates partielles sur la date de naissance des tiers
		binder.registerCustomEditor(RegDate.class, "tiers.dateNaissance", new RegDateEditor(true, true));
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		TiersEditView bean = (TiersEditView) command;
		checkAccesDossierEnEcriture(bean.getTiers().getNumero());

		if (request.getParameter(BUTTON_SAVE) != null) {
			this.setModified(false);
			Tiers tierSaved = tiersEditManager.save(bean);
			return new ModelAndView("redirect:../tiers/visu.do?id=" + tierSaved.getId());
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
