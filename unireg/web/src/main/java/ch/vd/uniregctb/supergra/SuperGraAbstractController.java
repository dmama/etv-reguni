package ch.vd.uniregctb.supergra;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;

import ch.vd.uniregctb.common.CommonSimpleFormController;

public abstract class SuperGraAbstractController extends CommonSimpleFormController {

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

	protected boolean handleCommonAction(HttpServletRequest request) {
		return handleDeltaDelete(request) || handleToggleShowDetails(request) || handleRollbackAll(request) || handleCommitAll(request);
	}

	private boolean handleDeltaDelete(HttpServletRequest request) {

		final String delDelta = request.getParameter("delDelta");
		if (StringUtils.isNotBlank(delDelta)) {

			final int index = Integer.parseInt(delDelta);
			final SuperGraSession session = getSession(request);
			final Delta action = session.getDeltas().remove(index);

			flash("L'action \"" + action + "\" a été supprimée.");
			return true;
		}

		return false;
	}

	private boolean handleRollbackAll(HttpServletRequest request) {

		final String rollback = request.getParameter("rollbackAll");
		if (StringUtils.isNotBlank(rollback)) {

			final SuperGraSession session = getSession(request);
			session.getDeltas().clear();

			flash("Toutes les actions ont été supprimées.");
			return true;
		}

		return false;
	}

	private boolean handleCommitAll(HttpServletRequest request) {

		final String commit = request.getParameter("commitAll");
		if (StringUtils.isNotBlank(commit)) {

			final SuperGraSession session = getSession(request);
			final int size = session.getDeltas().size();
			if (size <= 0) {
				flashWarning("Il n'y a aucune modification en attente !");
			}
			else {
				// on applique et commit les deltas dans la DB
				manager.commitDeltas(session.getDeltas());

				// on efface les deltas appliqués dans la session
				session.getDeltas().clear();

				if (size == 1) {
					flash("La modification a été sauvegardée dans la base de données.");
				}
				else {
					flash("Les " + size + " modifications ont été sauvegardées dans la base de données.");
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

			flash("Les détails sont maintenant " + (show ? "visibles" : "masqués") + ".");
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
