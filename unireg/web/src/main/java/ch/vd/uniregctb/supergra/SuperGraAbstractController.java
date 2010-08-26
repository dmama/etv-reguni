package ch.vd.uniregctb.supergra;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.web.servlet.mvc.SimpleFormController;

public abstract class SuperGraAbstractController extends SimpleFormController {

	protected SuperGraSession getSession(HttpServletRequest request) {
		SuperGraSession session = (SuperGraSession) request.getSession().getAttribute("superGraSession");
		if (session == null) {
			session = new SuperGraSession();
			request.getSession().setAttribute("superGraSession", session);
		}
		return session;
	}

	/**
	 * Renseigne un message pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param request la requête courante
	 * @param message le message
	 */
	protected void flash(HttpServletRequest request, String message) {
		FlashMessage flash = (FlashMessage) request.getSession().getAttribute("flash");
		if (flash == null) {
			flash = new FlashMessage();
			request.getSession().setAttribute("flash", flash);
		}
		flash.setMessage(message);
	}

	/**
	 * Renseigne un message de warning pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param request la requête courante
	 * @param message le message
	 */
	protected void flashWarning(HttpServletRequest request, String message) {
		FlashMessage flash = (FlashMessage) request.getSession().getAttribute("flash");
		if (flash == null) {
			flash = new FlashMessage();
			request.getSession().setAttribute("flash", flash);
		}
		flash.setWarning(message);
	}

	/**
	 * Renseigne un message d'erreur pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param request la requête courante
	 * @param message le message
	 */
	protected void flashError(HttpServletRequest request, String message) {
		FlashMessage flash = (FlashMessage) request.getSession().getAttribute("flash");
		if (flash == null) {
			flash = new FlashMessage();
			request.getSession().setAttribute("flash", flash);
		}
		flash.setError(message);
	}

	protected boolean handleCommonAction(HttpServletRequest request) {
		return handleDeltaDelete(request) || handleToggleShowDetails(request);
	}

	private boolean handleDeltaDelete(HttpServletRequest request) {

		final String delDelta = request.getParameter("delDelta");
		if (StringUtils.isNotBlank(delDelta)) {

			final int index = Integer.parseInt(delDelta);
			final SuperGraSession session = getSession(request);
			final Delta action = session.getDeltas().remove(index);

			flash(request, "L'action \"" + action + "\" a été supprimée.");
			return true;
		}

		return false;
	}

	private boolean handleToggleShowDetails(HttpServletRequest request) {

		final String showDetails = request.getParameter("showDetails");
		final String oldShowDetails = request.getParameter("_showDetails");
		if (StringUtils.isNotBlank(showDetails) || StringUtils.isNotBlank(oldShowDetails)) {

			final boolean show = parseBoolean(showDetails);
			final SuperGraSession session = getSession(request);
			session.getOptions().setShowDetails(show);

			flash(request, "Les détails sont maintenant " + (show ? "visibles" : "masqués") + ".");
			return true;
		}

		return false;
	}

	private static boolean parseBoolean(String showDetails) {
		final CustomBooleanEditor editor = new CustomBooleanEditor(true);
		editor.setAsText(showDetails);
		final Boolean value = (Boolean)editor.getValue();
		return (value == null ? false : value);
	}
}
