package ch.vd.uniregctb.supergra;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ch.vd.uniregctb.utils.UniregModeHelper;

public abstract class SuperGraAbstractController extends SimpleFormController {

	protected static final String ACCESS_DENIED = "Cet écran nécessite le droit d'accès spécial Super-Gra !";

	protected SuperGraManager manager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(SuperGraManager manager) {
		this.manager = manager;
	}

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
		return handleDeltaDelete(request) || handleToggleShowDetails(request) || handleRollbackAll(request) || handleCommitAll(request);
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

	private boolean handleRollbackAll(HttpServletRequest request) {

		final String rollback = request.getParameter("rollbackAll");
		if (StringUtils.isNotBlank(rollback)) {

			final SuperGraSession session = getSession(request);
			session.getDeltas().clear();

			flash(request, "Toutes les actions ont été supprimées.");
			return true;
		}

		return false;
	}

	private boolean handleCommitAll(HttpServletRequest request) {

		final String commit = request.getParameter("commitAll");
		if (StringUtils.isNotBlank(commit)) {

			// FIXME (msi) ajouter un profile SuperGra et l'utiliser
			if (!UniregModeHelper.isTestMode()) {
				flashError(request, "La modification des données en mode SuperGra n'est pas autorisée en dehors des environnements de test pour l'instant.");
				return true;
			}

			final SuperGraSession session = getSession(request);
			final int size = session.getDeltas().size();
			if (size <= 0) {
				flashWarning(request, "Il n'y a aucune modification en attente !");
			}
			else {
				// on applique et commit les deltas dans la DB
				manager.commitDeltas(session.getDeltas());

				// on efface les deltas appliqués dans la session
				session.getDeltas().clear();

				if (size == 1) {
					flash(request, "La modification a été sauvegardée dans la base de données.");
				}
				else {
					flash(request, "Les " + size + " modifications ont été sauvegardées dans la base de données.");
				}
			}
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
		final Boolean value = (Boolean) editor.getValue();
		return (value == null ? false : value);
	}
}
