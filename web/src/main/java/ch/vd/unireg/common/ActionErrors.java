package ch.vd.unireg.common;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Classe utilitaire pour l'affichage des messages d'erreurs suite aux actions dans les contrôleurs web Spring 3.
 */
public abstract class ActionErrors {

	/**
	 * Ajoute un message de warning suite à une action de l'utilisateur. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 */
	public static void addWarning(String message) {
		final ActionMessage g = new ActionMessage();
		g.setWarning(message);
		getList().add(g);
	}

	/**
	 * Ajoute un message d'erreur suite à une action de l'utilisateur. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 */
	public static void addError(String message) {
		final ActionMessage g = new ActionMessage();
		g.setError(message);
		getList().add(g);
	}

	@SuppressWarnings("unchecked")
	@NotNull
	private static ActionMessageList getList() {
		final RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		ActionMessageList list = (ActionMessageList) attributes.getAttribute("actionErrors", RequestAttributes.SCOPE_SESSION);
		if (list == null) {
			list = new ActionMessageList();
			attributes.setAttribute("actionErrors", list, RequestAttributes.SCOPE_SESSION);
		}
		list.setActive(true);
		return list;
	}

}
