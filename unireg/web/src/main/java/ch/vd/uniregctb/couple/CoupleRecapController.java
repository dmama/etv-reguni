package ch.vd.uniregctb.couple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.couple.manager.CoupleRecapManager;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.tiers.MenageCommun;

public class CoupleRecapController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(CoupleRecapController.class);

	private final String NUMERO_PREMIER_PP_PARAMETER_NAME = "numeroPP1";
	private final String NUMERO_SECOND_PP_PARAMETER_NAME = "numeroPP2";

	public static final String PAGE_TITLE = "pageTitle";
	private static final String TITLE_PREFIX = "title.recapitulatif.";

	public static final String CONFIRMATION_MSG = "coupleConfirmationMsg";
	private static final String CONFIRMATION_PREFIX = "message.confirm.";

	private final String NUMERO_CTB_PARAMETER_NAME = "numeroCTB";

	private CoupleRecapManager coupleRecapManager;

	public CoupleRecapManager getCoupleRecapManager() {
		return coupleRecapManager;
	}

	public void setCoupleRecapManager(CoupleRecapManager coupleRecapManager) {
		this.coupleRecapManager = coupleRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroPP1Param = request.getParameter(NUMERO_PREMIER_PP_PARAMETER_NAME);
		Long numeroPP1 = Long.parseLong(numeroPP1Param);
		checkAccesDossierEnLecture(numeroPP1);
		String numeroPP2Param = request.getParameter(NUMERO_SECOND_PP_PARAMETER_NAME);
		Long numeroPP2 = null;
		if (numeroPP2Param != null) {
			numeroPP2 = Long.parseLong(numeroPP2Param);
			checkAccesDossierEnLecture(numeroPP2);
		}

		String numeroCTBParam = request.getParameter(NUMERO_CTB_PARAMETER_NAME);
		Long numeroCTB = null;
		if (numeroCTBParam != null) {
			numeroCTB = Long.parseLong(numeroCTBParam);
			checkAccesDossierEnLecture(numeroCTB);
		}
		CoupleRecapView coupleRecapView;
		if (numeroCTB != null) {
			coupleRecapView = coupleRecapManager.get(numeroPP1, numeroPP2, numeroCTB);
		}
		else if (numeroPP2 != null) {
			coupleRecapView = coupleRecapManager.get(numeroPP1, numeroPP2);
		}
		else {
			coupleRecapView = coupleRecapManager.get(numeroPP1);
		}

		return coupleRecapView;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map referenceData(HttpServletRequest request, Object command, Errors errors) throws Exception {
		Map data = new HashMap();
		CoupleRecapView coupleRecapView = (CoupleRecapView) command;
		List<String> warnings = new ArrayList<String>();

		String warningChgFor = "Mode d'imposition du contribuable ménage";
		String warningChgCommune = "Commune du for principal pour le contribuable ménage";

		switch (coupleRecapView.getTypeUnion()) {
			case COUPLE:
				data.put(PAGE_TITLE, TITLE_PREFIX + "couple");
				data.put(CONFIRMATION_MSG, CONFIRMATION_PREFIX + "couple");
				break;
			case SEUL:
				data.put(PAGE_TITLE, TITLE_PREFIX + "seul");
				data.put(CONFIRMATION_MSG, CONFIRMATION_PREFIX + "seul");
				break;
			case RECONCILIATION:
				data.put(PAGE_TITLE, TITLE_PREFIX + "reconciliation");
				data.put(CONFIRMATION_MSG, CONFIRMATION_PREFIX + "reconciliation");
				break;
			case RECONSTITUTION_MENAGE:
				data.put(PAGE_TITLE, TITLE_PREFIX + "reconstitution");
				data.put(CONFIRMATION_MSG, CONFIRMATION_PREFIX + "reconstitution");
				warnings.add(warningChgFor);
				warnings.add(warningChgCommune);
				break;
			case FUSION_MENAGES:
				data.put(PAGE_TITLE, TITLE_PREFIX + "fusion");
				data.put(CONFIRMATION_MSG, CONFIRMATION_PREFIX + "fusion");
				warnings.add(warningChgFor);
				warnings.add(warningChgCommune);
				break;
		}
		data.put("warnings", warnings);
		return data;
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

		CoupleRecapView coupleRecapView = (CoupleRecapView) command;
		checkAccesDossierEnEcriture(coupleRecapView.getPremierePersonne().getNumero());
		if (coupleRecapView.getSecondePersonne() != null) {
			checkAccesDossierEnEcriture(coupleRecapView.getSecondePersonne().getNumero());
		}

		ContribuableListController.effaceCriteresRecherche(request);

		MenageCommun menageCommun = coupleRecapManager.save(coupleRecapView);
		return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + menageCommun.getNumero(), true));
	}

}
