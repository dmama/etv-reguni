package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.AdresseManager;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;

public class AdressesEditController extends AbstractTiersController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(AdressesEditController.class);

	public final static String TARGET_ANNULER_ADRESSE = "annulerAdresse";

	private TiersEditManager tiersEditManager;

	private AdresseManager adresseManager;

	public TiersEditManager getTiersEditManager() {
		return tiersEditManager;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	public AdresseManager getAdresseManager() {
		return adresseManager;
	}

	public void setAdresseManager(AdresseManager adresseManager) {
		this.adresseManager = adresseManager;
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
				tiersView = adresseManager.getView(id);
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
		checkAccesDossierEnEcriture(bean.getTiers().getNumero());

		if (TARGET_ANNULER_ADRESSE.equals(getTarget())) {
			String idAdresseParam = getEventArgument();
			if (idAdresseParam != null) {
				Long idAdresse = Long.parseLong(idAdresseParam);
				adresseManager.annulerAdresse(idAdresse);
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
