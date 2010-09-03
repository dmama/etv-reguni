package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ch.vd.uniregctb.supergra.FlashMessage;

/**
 * Classe commune à tous les simple form controller utilisés dans Unireg.
 */
public abstract class CommonSimpleFormController extends SimpleFormController {

	/**
	 * Cette variable contient la session courante, pour utilisation privée par les méthodes 'flash'.
	 */
	private final ThreadLocal<HttpServletRequest> request = new ThreadLocal<HttpServletRequest>();

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		this.request.set(request);
		return super.handleRequest(request, response);
	}

	/**
	 * Renseigne un message pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 */
	protected void flash(String message) {
		final HttpSession session = request.get().getSession();
		FlashMessage flash = (FlashMessage) session.getAttribute("flash");
		if (flash == null) {
			flash = new FlashMessage();
			session.setAttribute("flash", flash);
		}
		flash.setMessage(message);
	}

	/**
	 * Renseigne un message de warning pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 */
	protected void flashWarning(String message) {
		final HttpSession session = request.get().getSession();
		FlashMessage flash = (FlashMessage) session.getAttribute("flash");
		if (flash == null) {
			flash = new FlashMessage();
			session.setAttribute("flash", flash);
		}
		flash.setWarning(message);
	}

	/**
	 * Renseigne un message d'erreur pour la zone flash de l'écran. Le message sera affiché une seule fois puis mis à null.
	 *
	 * @param message le message
	 */
	protected void flashError(String message) {
		final HttpSession session = request.get().getSession();
		FlashMessage flash = (FlashMessage) session.getAttribute("flash");
		if (flash == null) {
			flash = new FlashMessage();
			session.setAttribute("flash", flash);
		}
		flash.setError(message);
	}
}
