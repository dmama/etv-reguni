package ch.vd.uniregctb.param;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

/**
 *
 * Factorise du code en commun pour le package courant
 *
 * @author xsifnr
 *
 */
class Commun {

//	private static final String PARAMETER_CALLBACK = "cb";
	private static final String PARAMETER_PERIODE_ID = "pf";
	private static final String PARAMETER_MODELE_ID = "md";
	private static final String PARAMETER_FEUILLE_ID = "mfd";

	static final ModelAndView REDIRECT_TO_PERIODE = new ModelAndView(new RedirectView("periode.do"));



	static void verifieLesDroits() throws AccessDeniedException {
		//gestion des droits
		if(!SecurityProvider.isGranted(Role.PARAM_PERIODE)){
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec sur l'écran de paramétrisation des périodes");
		}
	}

	static Long getPeriodeIdFromRequest (HttpServletRequest request) {
		return Long.parseLong(getParameter(request, PARAMETER_PERIODE_ID));
	}

	static boolean isPeriodeIdInRequest (HttpServletRequest request) {
		return getParameter(request, PARAMETER_PERIODE_ID, false) != null;
	}

	static Long getModeleIdFromRequest (HttpServletRequest request) {
		return Long.parseLong(getParameter(request, PARAMETER_MODELE_ID));
	}

	static boolean isModeleIdInRequest (HttpServletRequest request) {
		return getParameter(request, PARAMETER_MODELE_ID, false) != null;
	}

	static Long getFeuilleIdFromRequest (HttpServletRequest request) {
		return Long.parseLong(getParameter(request, PARAMETER_FEUILLE_ID));
	}

	static boolean isFeuilleIdInRequest (HttpServletRequest request) {
		return getParameter(request, PARAMETER_FEUILLE_ID, false) != null;
	}


//	static boolean isCallback (HttpServletRequest request) {
//		return "x".equals(request.getParameter(PARAMETER_CALLBACK));
//	}

	private static String getParameter (HttpServletRequest request, String param) {
		return getParameter(request, param, true);
	}

	private static String getParameter (HttpServletRequest request, String param, boolean required) {
		String paramValue = request.getParameter(param);
		if (required && (paramValue == null || paramValue.trim().length() == 0)) {
			throw new RuntimeException(
					String.format("Parametre de request '%s' manquant", param)
			);
		}
		return paramValue;
	}

	static ModelAndView getModelAndViewToPeriode(Long idPeriode) {
		final String url = String.format("periode.do?%s=%s", PARAMETER_PERIODE_ID, idPeriode);
		return new ModelAndView(new RedirectView(url));
	}

	static ModelAndView getModelAndViewToPeriode(Long periodeId, Long modeleId) {
		final String url = String.format("periode.do?%s=%s&%s=%s", PARAMETER_PERIODE_ID, periodeId, PARAMETER_MODELE_ID, modeleId);
		return new ModelAndView(new RedirectView(url));
	}

	static ModelAndView getModelAndViewToPeriode () {
		return REDIRECT_TO_PERIODE;
	}

	private Commun() {}

}
