package ch.vd.uniregctb.tiers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.tiers.manager.MouvementVisuManager;

/**
 * Controller pour l'overlay de visualisation du détail du mouvement
 *
 * @author xcifde
 *
 */
public class MouvementVisuController extends AbstractTiersController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(MouvementVisuController.class);

	private static final String ID_MVT_PARAM = "idMvt";

	private MouvementVisuManager mouvementVisuManager;

	public MouvementVisuManager getMouvementVisuManager() {
		return mouvementVisuManager;
	}

	public void setMouvementVisuManager(MouvementVisuManager mouvementVisuManager) {
		this.mouvementVisuManager = mouvementVisuManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		MouvementDetailView mvtView = null;

		final String idMvtParam = request.getParameter(ID_MVT_PARAM);
		if (idMvtParam != null) {
			final Long id = Long.parseLong(idMvtParam);
			if (idMvtParam != null && !"".equals(idMvtParam)) {
				mvtView = mouvementVisuManager.get(id);
			}
		}

		return mvtView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
		throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		return mav;
	}




}
