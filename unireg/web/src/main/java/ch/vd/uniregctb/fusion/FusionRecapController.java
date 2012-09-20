package ch.vd.uniregctb.fusion;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.fusion.manager.FusionRecapManager;
import ch.vd.uniregctb.fusion.view.FusionRecapView;

public class FusionRecapController  extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(FusionRecapController.class);

	private static final String NUMERO_NON_HABITANT_PARAMETER_NAME = "numeroNonHab";
	private static final String NUMERO_HABITANT_NAME = "numeroHab";

	private FusionRecapManager fusionRecapManager;

	public FusionRecapManager getFusionRecapManager() {
		return fusionRecapManager;
	}

	public void setFusionRecapManager(FusionRecapManager fusionRecapManager) {
		this.fusionRecapManager = fusionRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroNonHabParam = request.getParameter(NUMERO_NON_HABITANT_PARAMETER_NAME);
		String numeroHabParam = request.getParameter(NUMERO_HABITANT_NAME);
		Long numeroNonHab = Long.parseLong(numeroNonHabParam);
		Long numeroHab = Long.parseLong(numeroHabParam);

		checkAccesDossierEnLecture(numeroNonHab);
		checkAccesDossierEnLecture(numeroHab);

		FusionRecapView  fusionRecapView = fusionRecapManager.get(numeroNonHab, numeroHab);

		return fusionRecapView;
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
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		FusionRecapView fusionRecapView = (FusionRecapView) command;
		checkAccesDossierEnEcriture(fusionRecapView.getHabitant().getNumero());
		checkAccesDossierEnLecture(fusionRecapView.getNonHabitant().getNumero());

		fusionRecapManager.save(fusionRecapView);

		mav.setView(new RedirectView("../tiers/visu.do?id=" + fusionRecapView.getHabitant().getNumero()));


		return mav;
	}

}
