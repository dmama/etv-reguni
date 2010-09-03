package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.SimpleFormController;

import ch.vd.uniregctb.supergra.FlashMessage;

/**
 * Classe commune à tous les simple form controller utilisés dans Unireg.
 */
public abstract class CommonSimpleFormController extends SimpleFormController {

	/**
	 * Renseigne un message pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param request la requête courante
	 * @param message le message
	 */
	protected static void flash(HttpServletRequest request, String message) {
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
	protected static void flashWarning(HttpServletRequest request, String message) {
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
	protected static void flashError(HttpServletRequest request, String message) {
		FlashMessage flash = (FlashMessage) request.getSession().getAttribute("flash");
		if (flash == null) {
			flash = new FlashMessage();
			request.getSession().setAttribute("flash", flash);
		}
		flash.setError(message);
	}
}
