package ch.vd.moscow.controller;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Classe utilitaire pour l'affichage des messages <i>flash</i> dans les contrôleurs web Spring 3.
 */
public abstract class Flash {

	/**
	 * Renseigne un message pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 */
	public static void message(String message) {
		getFlash().setMessage(message);
	}

	/**
	 * Renseigne un message pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 * @param timeout timeout, en millisecondes, au delà duquel le message peut-être à nouveau effacé (0 pour l'absence de timeout)
	 */
	public static void message(String message, long timeout) {
		getFlash().setMessage(message, timeout);
	}

	/**
	 * Renseigne un message de warning pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 */
	public static void warning(String message) {
		getFlash().setWarning(message);
	}

	/**
	 * Renseigne un message de warning pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 * @param timeout timeout, en millisecondes, au delà duquel le message peut-être à nouveau effacé (0 pour l'absence de timeout)
	 */
	public static void warning(String message, long timeout) {
		getFlash().setWarning(message, timeout);
	}

	/**
	 * Renseigne un message d'erreur pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 */
	public static void error(String message) {
		getFlash().setError(message);
	}

	/**
	 * Renseigne un message d'erreur pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 * @param timeout timeout, en millisecondes, au delà duquel le message peut-être à nouveau effacé (0 pour l'absence de timeout)
	 */
	public static void error(String message, long timeout) {
		getFlash().setError(message, timeout);
	}

	private static FlashMessage getFlash() {
		final RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		FlashMessage flash = (FlashMessage) attributes.getAttribute("flash", RequestAttributes.SCOPE_SESSION);
		if (flash == null) {
			flash = new FlashMessage();
			attributes.setAttribute("flash", flash, RequestAttributes.SCOPE_SESSION);
		}
		return flash;
	}

}
