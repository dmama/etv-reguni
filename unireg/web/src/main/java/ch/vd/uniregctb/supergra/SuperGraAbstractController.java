package ch.vd.uniregctb.supergra;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.CommonSimpleFormController;
import ch.vd.uniregctb.supergra.delta.AddSubEntity;
import ch.vd.uniregctb.supergra.delta.Delta;

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

	/**
	 * Traite les actions communes entre les différents contrôleurs du mode SuperGra.
	 *
	 * @param request    la requête http courante
	 * @param currentKey l'entité couramment affichée
	 * @return <b>null</b> si aucune action commune n'a été traitée; {@link ch.vd.uniregctb.supergra.SuperGraAbstractController#OK_CONTINUE} si l'action a été traitée et qu'il faut réafficher la page
	 *         courante; ou une instance de {@link ch.vd.uniregctb.supergra.SuperGraAbstractController.OkRedirect} si l'action a été traitée mais qu'il faut afficher une nouvelle page.
	 */
	protected HandleResponse handleCommonAction(HttpServletRequest request, EntityKey currentKey) {
		HandleResponse response = handleDeltaDelete(request);
		if (response == null) response = handleToggleShowDetails(request);
		if (response == null) response = handleRollbackAll(request);
		if (response == null) response = handleCommitAll(request, currentKey);
		return response;
	}

	private HandleResponse handleDeltaDelete(HttpServletRequest request) {

		final String delDelta = request.getParameter("delDelta");
		if (StringUtils.isNotBlank(delDelta)) {

			final int index = Integer.parseInt(delDelta);
			final SuperGraSession session = getSession(request);
			final Delta action = session.removeDelta(index);

			flash("L'action \"" + action + "\" a été supprimée.");
			return OK_CONTINUE;
		}

		return null;
	}

	private HandleResponse handleRollbackAll(HttpServletRequest request) {

		final String rollback = request.getParameter("rollbackAll");
		if (StringUtils.isNotBlank(rollback)) {

			final SuperGraSession session = getSession(request);
			session.clearDeltas();

			flash("Toutes les actions ont été supprimées.");
			return OK_CONTINUE;
		}

		return null;
	}

	protected static final OkContinue OK_CONTINUE = new OkContinue();

	protected static abstract class HandleResponse {
	}

	/**
	 * Réponse qui signifie que la réponse à été traitée correctement et qu'il faut simplement réafficher la page telle quelle.
	 */
	protected static class OkContinue extends HandleResponse {

	}

	/**
	 * Réponse qui signifie que la réponse à été traitée correctement mais qu'il faut rediriger l'utilisateur vers une autre entité
	 */
	protected static class OkRedirect extends HandleResponse {
		public final ModelAndView mav;

		public OkRedirect(ModelAndView mav) {
			this.mav = mav;
		}
	}

	private HandleResponse handleCommitAll(HttpServletRequest request, EntityKey currentKey) {

		final String commit = request.getParameter("commitAll");
		if (StringUtils.isNotBlank(commit)) {

			final SuperGraSession session = getSession(request);
			final int size = session.deltaSize();
			if (size <= 0) {
				flashWarning("Il n'y a aucune modification en attente !");
				return OK_CONTINUE;
			}

			final List<Delta> delta = session.getDeltas();
			boolean needRedirect = isEntityANewOne(currentKey, delta);

			// on applique et commit les deltas dans la DB
			manager.commitDeltas(delta);

			// on efface les deltas appliqués dans la session
			session.clearDeltas();

			if (size == 1) {
				flash("La modification a été sauvegardée dans la base de données.");
			}
			else {
				flash("Les " + size + " modifications ont été sauvegardées dans la base de données.");
			}

			if (needRedirect) {
				// si l'objet affiché couramment est un nouvel objet, il faut rediriger l'utilisateur sur le dernier tiers connu parce qu'Hibernate va
				// réassigner un id à l'objet courant et on ne sait pas lequel
				return new OkRedirect(new ModelAndView(new RedirectView("entity.do?id=" + session.getLastKnownTiersId() + "&class=" + EntityType.Tiers)));
			}
			else {
				return OK_CONTINUE;
			}
		}

		return null;
	}

	/**
	 * Détermine si l'entité spécifiée est une nouvelle entité (= entité créée à travers la session SuperGra courante)
	 *
	 * @param key   la clé d'une entité
	 * @param delta la liste des deltas à scanner
	 * @return <b>vrai</b> si l'entité spécifiée est une nouvelle entité; <b>faux</b> autement.
	 */
	private boolean isEntityANewOne(EntityKey key, List<Delta> delta) {
		boolean newEntity = false;
		for (Delta d : delta) {
			if (d instanceof AddSubEntity) {
				final AddSubEntity add = (AddSubEntity) d;
				if (add.getSubKey().equals(key)) {
					newEntity = true;
					break;
				}
			}
		}
		return newEntity;
	}

	private HandleResponse handleToggleShowDetails(HttpServletRequest request) {

		final String showDetails = request.getParameter("showDetails");
		final String oldShowDetails = request.getParameter("_showDetails");
		if (StringUtils.isNotBlank(showDetails) || StringUtils.isNotBlank(oldShowDetails)) {

			final boolean show = parseBoolean(showDetails);
			final SuperGraSession session = getSession(request);
			session.getOptions().setShowDetails(show);

			flash("Les détails sont maintenant " + (show ? "visibles" : "masqués") + ".");
			return OK_CONTINUE;
		}

		return null;
	}

	private static boolean parseBoolean(String showDetails) {
		final CustomBooleanEditor editor = new CustomBooleanEditor(true);
		editor.setAsText(showDetails);
		final Boolean value = (Boolean) editor.getValue();
		return (value == null ? false : value);
	}
}
