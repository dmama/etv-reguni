package ch.vd.uniregctb.separation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.separation.manager.SeparationRecapManager;
import ch.vd.uniregctb.separation.view.SeparationRecapView;

public class SeparationRecapController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(SeparationRecapController.class);

	private static final String NUMERO_COUPLE_PARAMETER_NAME = "numeroCple";

	private SeparationRecapManager separationRecapManager;

	public SeparationRecapManager getSeparationRecapManager() {
		return separationRecapManager;
	}

	public void setSeparationRecapManager(SeparationRecapManager separationRecapManager) {
		this.separationRecapManager = separationRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroCpleParam = request.getParameter(NUMERO_COUPLE_PARAMETER_NAME);
		Long numeroCple = Long.parseLong(numeroCpleParam);
		checkAccesDossierEnLecture(numeroCple);

		SeparationRecapView separationRecapView = separationRecapManager.get(numeroCple);

		return separationRecapView;
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
		SeparationRecapView separationRecapView = (SeparationRecapView) command;
		checkAccesDossierEnEcriture(separationRecapView.getCouple().getNumero());

		/*
		 * Ces warnings sont le résultat de la validation métier. Ils sont stockés temporairement dans 
		 * la session et effacés une fois la page de visualisation rendue au client.
		 */
		request.getSession().setAttribute("warnings", separationRecapView.getWarnings());

		try {
			separationRecapManager.save(separationRecapView);
		}
		catch (MetierServiceException e) {
			throw new ActionException(e.getMessage(), e);
		}
		return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + separationRecapView.getCouple().getNumero(), true));
	}
}
