package ch.vd.uniregctb.tiers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.tiers.view.ForFiscalView;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier donne.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersForController extends AbstractTiersController {

	private static final String ID_FOR_PARAMETER_NAME = "idFor";

	private static final String NUMERO_CTB_PARAMETER_NAME = "numero";

	private static final String NATURE_FOR_PARAMETER_NAME = "nature";

	private static final String NATURE_DPI_PARAMETER_VALUE = "DPI";

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(TiersForController.class);

	private ForFiscalManager forFiscalManager;

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		ForFiscalView forFiscalView = null;
		String idFor = request.getParameter(ID_FOR_PARAMETER_NAME);
		Long numeroCtb = extractLongParam(request, NUMERO_CTB_PARAMETER_NAME);
		String natureFor = request.getParameter(NATURE_FOR_PARAMETER_NAME);
		checkAccesDossierEnLecture(numeroCtb);

		//les droits sont vérifiés à la sauvegarde (ForFiscalValidator)
		if (idFor != null && !"".equals(idFor.trim())) {
			Long id = Long.parseLong(idFor);
			forFiscalView = forFiscalManager.get(id);
		}
		else {
			if (natureFor != null && NATURE_DPI_PARAMETER_VALUE.equals(natureFor.trim())) {
				forFiscalView = forFiscalManager.create(numeroCtb, true);
			} else {
				forFiscalView = forFiscalManager.create(numeroCtb, false);
			}
		}

		return forFiscalView;
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
		ForFiscalView forFiscalView = (ForFiscalView) command;
		checkAccesDossierEnEcriture(forFiscalView.getNumeroCtb());

		forFiscalManager.save(forFiscalView);
		return mav;
	}

	public ForFiscalManager getForFiscalManager() {
		return forFiscalManager;
	}

	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}


}
