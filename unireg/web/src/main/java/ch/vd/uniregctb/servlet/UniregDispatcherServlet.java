package ch.vd.uniregctb.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.IdentityWrapper;

/**
 * Dispatcher servlet dans lequel les exceptions "UnexpectedRollbackException" sont trappées et remontées sous la forme de leur rootCause
 * (ceci pour attrapper les erreurs de validation qui sautent très tard dans le processus transactionnel)
 */
public class UniregDispatcherServlet extends DispatcherServlet {

	private final Map<IdentityWrapper<HandlerAdapter>, HandlerAdapter> wrapping = new HashMap<>();

	private HandlerAdapter getWrapping(HandlerAdapter ha) {
		final IdentityWrapper<HandlerAdapter> key = new IdentityWrapper<>(ha);
		synchronized (wrapping) {
			HandlerAdapter wrapper = wrapping.get(key);
			if (wrapper == null) {
				wrapper = new UnexpectedRollbackAwareHandlerAdapter(ha);
				wrapping.put(key, wrapper);
			}
			return wrapper;
		}
	}

	/**
	 * Wrapper de {@link HandlerAdapter} qui va trapper les exceptions {@link UnexpectedRollbackException} lancée
	 * par la méthode {@link #handle} pour en extraire la cause première et la transmettre plus loin
	 */
	private static final class UnexpectedRollbackAwareHandlerAdapter implements HandlerAdapter {

		private final HandlerAdapter target;

		private UnexpectedRollbackAwareHandlerAdapter(HandlerAdapter target) {
			this.target = target;
		}

		@Override
		public boolean supports(Object handler) {
			return target.supports(handler);
		}

		@Override
		public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
			try {
				return target.handle(request, response, handler);
			}
			catch (UnexpectedRollbackException e) {
				@SuppressWarnings("ThrowableResultOfMethodCallIgnored") final Throwable rootCause = ExceptionUtils.getRootCause(e);
				if (rootCause instanceof Exception) {
					throw (Exception) rootCause;
				}
				else {
					throw e;
				}
			}
		}

		@Override
		public long getLastModified(HttpServletRequest request, Object handler) {
			return target.getLastModified(request, handler);
		}
	}

	@Override
	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		final HandlerAdapter ha = super.getHandlerAdapter(handler);
		return getWrapping(ha);
	}
}
