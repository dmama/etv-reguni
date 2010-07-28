package ch.vd.uniregctb.tiers;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.action.prototype.HideElement;
import org.springmodules.xt.ajax.action.prototype.ShowElement;
import org.springmodules.xt.ajax.component.Component;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.parametrage.ParametreEnum;
import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

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
	private ParametreAppService paramService;

	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		final Map<String, Object> referenceData = new HashMap<String, Object>();
		referenceData.put(ParametreEnum.anneeMinimaleForDebiteur.name(), paramService.getAnneeMinimaleForDebiteur());
		return referenceData;
	}

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

		if (forFiscalView.getId() == null) {
			forFiscalManager.addFor(forFiscalView);
		}
		else if (forFiscalView.isChangementModeImposition()) {
			forFiscalManager.updateModeImposition(forFiscalView);
		}
		else {
			forFiscalManager.updateFor(forFiscalView);
		}

		return new ModelAndView("redirect:../fiscal/edit.do?id=" + forFiscalView.getNumeroCtb());
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParamService(ParametreAppService paramService) {
		this.paramService = paramService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public AjaxResponse buildSynchronizeActionsTableSurModificationDeFor(AjaxActionEvent event) throws ParseException {

		Component component;
		try {
			final Map<String, String> parameters = event.getParameters();
			final String motifOuvertureAsString = parameters.get("motifOuverture");
			final String dateOuvertureAsString = parameters.get("dateOuverture");
			final String motifFermetureAsString = parameters.get("motifFermeture");
			final String dateFermetureAsString = parameters.get("dateFermeture");
			final String noOfsAutoriteAsString = parameters.get("nOfsAutoriteFiscale").replaceAll("[^0-9]", "");

			if (StringUtils.isBlank(dateOuvertureAsString) || StringUtils.isBlank(motifOuvertureAsString) || StringUtils.isBlank(noOfsAutoriteAsString)) {
				return null;
			}

			final Long forId = Long.valueOf(parameters.get("forId"));
			final RegDate dateOuverture = RegDateHelper.displayStringToRegDate(dateOuvertureAsString, false);
			final MotifFor motifOuverture = MotifFor.valueOf(motifOuvertureAsString);
			final RegDate dateFermeture;
			final MotifFor motifFermeture;
			if (StringUtils.isNotBlank(dateFermetureAsString) && StringUtils.isNotBlank(motifFermetureAsString)) {
				dateFermeture = RegDateHelper.displayStringToRegDate(dateFermetureAsString, false);
				motifFermeture = MotifFor.valueOf(motifFermetureAsString);
			}
			else {
				dateFermeture = null;
				motifFermeture = null;
			}
			final int noOfsAutoriteFiscale = Integer.parseInt(noOfsAutoriteAsString);

			component = forFiscalManager.buildSynchronizeActionsTableSurModificationDeFor(forId, dateOuverture, motifOuverture, dateFermeture, motifFermeture, noOfsAutoriteFiscale);
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			component = null;
		}

		final AjaxResponse response = new AjaxResponseImpl();
		if (component == null) {
			response.addAction(new HideElement("actions_column"));
		}
		else {
			response.addAction(new ReplaceContentAction("actions_list", component));
			response.addAction(new ShowElement("actions_column"));
		}
		return response;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public AjaxResponse updateActionListSurModificationDuModeImposition(AjaxActionEvent event) throws ParseException {

		Component component;
		try {
			final Map<String, String> parameters = event.getParameters();
			final String modeImpositionAsString = parameters.get("modeImposition");
			final String motifChangementAsString = parameters.get("motifChangement");
			final String dateChangementAsString = parameters.get("dateChangement");

			if (StringUtils.isBlank(modeImpositionAsString) || StringUtils.isBlank(motifChangementAsString) || StringUtils.isBlank(dateChangementAsString)) {
				return null;
			}

			final Long forId = Long.valueOf(parameters.get("forId"));
			final RegDate dateChangement = RegDateHelper.displayStringToRegDate(dateChangementAsString, false);
			final ModeImposition modeImposition = ModeImposition.valueOf(modeImpositionAsString);
			final MotifFor motifChangement = MotifFor.valueOf(motifChangementAsString);

			component = forFiscalManager.buildSynchronizeActionsTableSurModificationDuModeImposition(forId, dateChangement, modeImposition, motifChangement);
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			component = null;
		}

		final AjaxResponse response = new AjaxResponseImpl();
		if (component == null) {
			response.addAction(new HideElement("actions_column"));
		}
		else {
			response.addAction(new ReplaceContentAction("actions_list", component));
			response.addAction(new ShowElement("actions_column"));
		}
		return response;
	}
}
