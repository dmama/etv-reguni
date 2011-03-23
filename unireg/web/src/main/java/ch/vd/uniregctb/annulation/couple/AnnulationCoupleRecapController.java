package ch.vd.uniregctb.annulation.couple;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.annulation.couple.manager.AnnulationCoupleRecapManager;
import ch.vd.uniregctb.annulation.couple.view.AnnulationCoupleRecapView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;

public class AnnulationCoupleRecapController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(AnnulationCoupleRecapController.class);

	private final String NUMERO_COUPLE_PARAMETER_NAME = "numeroCple";

	private AnnulationCoupleRecapManager annulationCoupleRecapManager;

	public AnnulationCoupleRecapManager getAnnulationCoupleRecapManager() {
		return annulationCoupleRecapManager;
	}

	public void setAnnulationCoupleRecapManager(AnnulationCoupleRecapManager annulationCoupleRecapManager) {
		this.annulationCoupleRecapManager = annulationCoupleRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroCpleParam = request.getParameter(NUMERO_COUPLE_PARAMETER_NAME);
		Long numeroCple = Long.parseLong(numeroCpleParam);

		checkAccesDossierEnLecture(numeroCple);

		AnnulationCoupleRecapView annulationCoupleRecapView = annulationCoupleRecapManager.get(numeroCple);

		return annulationCoupleRecapView;
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

		AnnulationCoupleRecapView annulationCoupleRecapView = (AnnulationCoupleRecapView) command;
		checkAccesDossierEnLecture(annulationCoupleRecapView.getCouple().getNumero());

		annulationCoupleRecapManager.save(annulationCoupleRecapView);
		return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + annulationCoupleRecapView.getCouple().getNumero(), true));
	}

}
