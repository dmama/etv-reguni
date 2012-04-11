package ch.vd.uniregctb.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.NotificationService;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.security.AccessDeniedException;

/**
 * Classe spécialisée qui notifie par email toute exception non-catchée par la servlet.
 *
 * @see http://developingdeveloper.wordpress.com/2008/03/09/handling-exceptions-in-spring-mvc-part-2/
 */
public class NotifyingExceptionResolver implements HandlerExceptionResolver, Ordered {

	private NotificationService notificationService;
	private String applicationName;
	private int order = Ordered.LOWEST_PRECEDENCE;

	/** map des exceptions (indexées par leur sha1) déjà envoyées + le nombre d'envois */
	private final Map<String, Integer> exceptions = new HashMap<String, Integer>();

	protected final Logger LOGGER = Logger.getLogger(NotifyingExceptionResolver.class);

	@SuppressWarnings("UnusedDeclaration")
	public void setNotificationService(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

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

		boolean ignoreException = (ex instanceof ObjectNotFoundException // exception levée lorsque l'utilisateur entre des données erronées
								|| ex instanceof AccessDeniedException   // exception levée lorsque l'utilisateur ne possède par de profil IfoSec
								|| ex instanceof ActionException         // exception catchée gracieusement au niveau de la classe de base des contrôleurs
								|| ex instanceof ValidationException);   // exception catchée gracieusement au niveau de la classe de base des contrôleurs


		if (!ignoreException) {
			LOGGER.warn("An exception has occured in the application", ex);

			final int occurenceCount = registerException(ex);

			// on notifie par email lors de la première exception, et à chaque nouvelle centaine d'exceptions
			if (occurenceCount == 1 || occurenceCount % 100 == 0) {
				String user;
				if (AuthenticationHelper.getAuthentication() == null) {
					user = AuthenticationHelper.SYSTEM_USER; // exception en dehors du domaine d'action des utilisateurs
				}
				else {
					user = AuthenticationHelper.getCurrentPrincipal();
				}
				notificationService.sendNotification(ex, DateHelper.getCurrentDate(), applicationName, request.getRequestURL().toString(), user,
						occurenceCount);
			}
		}

		return null;
	}

	/**
	 * Enregistre la nouvelle exception et retourne le nombre total de fois que cette exception a été vue.
	 *
	 * @param ex
	 *            l'exception à enregister
	 * @return le nombre total de fois que cette exception a été vue. Retourne <code>1</code> si l'exception est vue pour la première fois.
	 */
	private int registerException(Exception ex) {
		final String callstack = ExceptionUtils.extractCallStack(ex);
		final byte[] bytes = DigestUtils.sha(callstack);
		final String sha = new String(bytes);

		int occurenceCount = 1;

		synchronized (exceptions) {
			final Integer count = exceptions.get(sha);
			if (count != null) {
				occurenceCount = count + 1;
			}
			exceptions.put(sha, occurenceCount);
		}

		return occurenceCount;
	}

}
