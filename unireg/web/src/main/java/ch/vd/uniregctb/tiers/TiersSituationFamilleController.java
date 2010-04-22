package ch.vd.uniregctb.tiers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.SituationFamilleManager;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;

public class TiersSituationFamilleController extends AbstractTiersController {

	protected final Logger LOGGER = Logger.getLogger(TiersSituationFamilleController.class);

	private static final String NUMERO_CTB_PARAMETER_NAME = "numero";

	private SituationFamilleManager situationFamilleManager;

	public SituationFamilleManager getSituationFamilleManager() {
		return situationFamilleManager;
	}

	public void setSituationFamilleManager(SituationFamilleManager situationFamilleManager) {
		this.situationFamilleManager = situationFamilleManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		SituationFamilleView situationFamilleView = null;
		String numeroCtb = request.getParameter(NUMERO_CTB_PARAMETER_NAME);
		LOGGER.debug("Numero ctb :" + numeroCtb);
		if (numeroCtb != null && !"".equals(numeroCtb.trim())) {
			//gestion des droits de création d'une situation de famille par situationFamilleManager
			final Long id = Long.valueOf(numeroCtb);
			checkAccesDossierEnLecture(id);
			situationFamilleView = situationFamilleManager.create(id);
		}

		return situationFamilleView;
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

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		ModelAndView mav = super.onSubmit(request, response, command, errors);
		SituationFamilleView situationFamilleView = (SituationFamilleView) command;
		checkAccesDossierEnEcriture(situationFamilleView.getNumeroCtb());

		situationFamilleManager.save(situationFamilleView);

		return mav;
	}

}
