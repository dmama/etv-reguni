package ch.vd.uniregctb.annulation.couple;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.annulation.couple.manager.AnnulationCoupleRecapManager;
import ch.vd.uniregctb.annulation.couple.view.AnnulationCoupleRecapView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.metier.MetierServiceException;

public class AnnulationCoupleRecapController extends AbstractSimpleFormController {

	protected final Logger LOGGER = LoggerFactory.getLogger(AnnulationCoupleRecapController.class);

	private static final String NUMERO_COUPLE_PARAMETER_NAME = "numeroCple";

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
		final Long numeroCouple = annulationCoupleRecapView.getCouple().getNumero();
		checkAccesDossierEnLecture(numeroCouple);
		checkTraitementContribuableAvecDecisionAci(numeroCouple);

		try {
			annulationCoupleRecapManager.save(annulationCoupleRecapView);
		}
		catch (MetierServiceException e) {
			throw new ActionException(e.getMessage(), e);
		}
		return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + numeroCouple, true));
	}

}
