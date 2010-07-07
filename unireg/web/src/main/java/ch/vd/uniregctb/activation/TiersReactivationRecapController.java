package ch.vd.uniregctb.activation;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.activation.manager.TiersReactivationRecapManager;
import ch.vd.uniregctb.activation.view.TiersReactivationRecapView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;

public class TiersReactivationRecapController  extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(TiersReactivationRecapController.class);
	private final String NUMERO_PARAMETER_NAME = "numero";

	private TiersReactivationRecapManager tiersReactivationRecapManager;

	public void setTiersReactivationRecapManager(TiersReactivationRecapManager tiersReactivationRecapManager) {
		this.tiersReactivationRecapManager = tiersReactivationRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		Long numero = Long.parseLong(numeroParam);
		checkAccesDossierEnLecture(numero);

		TiersReactivationRecapView tiersReactivationRecapView = tiersReactivationRecapManager.get(numero);

		return tiersReactivationRecapView;
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
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		TiersReactivationRecapView tiersReactivationRecapView = (TiersReactivationRecapView) command;
		checkAccesDossierEnEcriture(tiersReactivationRecapView.getTiers().getNumero());

		tiersReactivationRecapManager.save(tiersReactivationRecapView);
		return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + tiersReactivationRecapView.getTiers().getNumero(), true));
	}


}
