package ch.vd.uniregctb.couple;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.couple.manager.CoupleRecapManager;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.utils.TiersNumberEditor;

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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCoupleRecapManager(CoupleRecapManager coupleRecapManager) {
		this.coupleRecapManager = coupleRecapManager;
	}

	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
		super.initBinder(request, binder);
		final Locale locale = request.getLocale();
		final NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(false); // pour éviter d'afficher des virgules dans le numéro de contribuable
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, "numeroTroisiemeTiers", new TiersNumberEditor(true));
	}

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

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {

		CoupleRecapView coupleRecapView = (CoupleRecapView) command;
		checkAccesDossierEnEcriture(coupleRecapView.getPremierePersonne().getNumero());
		if (coupleRecapView.getSecondePersonne() != null) {
			checkAccesDossierEnEcriture(coupleRecapView.getSecondePersonne().getNumero());
		}

		effaceCriteresRecherche(request);

		MenageCommun menageCommun = coupleRecapManager.save(coupleRecapView);
		return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + menageCommun.getNumero(), true));
	}

	public static final String CTB_CRITERIA_NAME = "ctbCriteria";

	/**
	 * Appelé depuis d'autres controlleurs pour effacer les critères de recherche utilisés
	 * lors de la recherche d'un contribuable non-habitant pour la constitution d'un couple
	 */
	public static void effaceCriteresRecherche(HttpServletRequest request) {
		HttpSession session = request.getSession(true);
		session.removeAttribute(CTB_CRITERIA_NAME);
	}
}
