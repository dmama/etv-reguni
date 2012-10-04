package ch.vd.uniregctb.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.tx.TxCallbackException;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.uniregctb.common.ActionErrors;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.metier.MetierServiceException;

/**
 * Ce resolver va détecter les erreurs de validation et d'action, et rediriger automatiquement l'appelant vers la page précédente en y ajoutant le détails des erreurs levées.
 */
public class ActionExceptionResolver implements HandlerExceptionResolver, Ordered {

	protected static final Logger LOGGER = Logger.getLogger(ActionExceptionResolver.class);

	private int order = Ordered.LOWEST_PRECEDENCE;

	@SuppressWarnings("UnusedDeclaration")
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		return handleException(request, ex);
	}

	private static ModelAndView handleException(HttpServletRequest request, Throwable ex) {

		final String referrer = request.getHeader("referer"); // Yes, with the legendary misspelling.

		ModelAndView mav = null;
		if (ex instanceof ValidationException) {
			LOGGER.debug("Validation exception catched -> redisplaying form : " + ex.getMessage());
			final ValidationException ve = (ValidationException) ex;
			for (ValidationMessage s : ve.getErrors()) {
				ActionErrors.addError(s.toString());
			}
			for (ValidationMessage s : ve.getWarnings()) {
				ActionErrors.addWarning(s.toString());
			}
			mav = new ModelAndView("redirect:" + referrer);
		}
		else if (ex instanceof ActionException) {
			LOGGER.debug("Action exception catched -> redisplaying form : " + ex.getMessage());
			final ActionException ae = (ActionException) ex;
			for (String s : ae.getErrors()) {
				ActionErrors.addError(s);
			}
			for (String s : ae.getWarnings()) {
				ActionErrors.addWarning(s);
			}
			mav = new ModelAndView("redirect:" + referrer);
		}
		else if (ex instanceof ObjectNotFoundException) {
			LOGGER.debug("ObjectNotFound exception catched -> redisplaying form : " + ex.getMessage());
			ActionErrors.addError(ex.getMessage());
			mav = new ModelAndView("redirect:" + referrer);
		}
		else if (ex instanceof MetierServiceException) {
			LOGGER.debug("MetierServiceException exception catched -> redisplaying form : " + ex.getMessage());
			ActionErrors.addError(ex.getMessage());
			mav = new ModelAndView("redirect:" + referrer);
		}
		else if (ex instanceof TxCallbackException) {
			final Throwable cause = ex.getCause();
			mav = handleException(request, cause);
		}
		else {
			// on ignore les autres erreurs
		}
		return mav;
	}
}
