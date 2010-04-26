package ch.vd.uniregctb.web.xt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springmodules.web.servlet.mvc.EnhancedSimpleFormController;
import org.springmodules.xt.ajax.AjaxAction;
import org.springmodules.xt.ajax.AjaxEvent;
import org.springmodules.xt.ajax.AjaxHandler;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.ElementMatcher;
import org.springmodules.xt.ajax.action.AppendContentAction;
import org.springmodules.xt.ajax.action.RemoveContentAction;
import org.springmodules.xt.ajax.action.matcher.WildcardMatcher;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.support.EventHandlingException;
import org.springmodules.xt.ajax.support.UnsupportedEventException;
import org.springmodules.xt.ajax.validation.DefaultValidationHandler;
import org.springmodules.xt.ajax.validation.ErrorRenderingCallback;
import org.springmodules.xt.ajax.validation.SuccessRenderingCallback;
import org.springmodules.xt.ajax.validation.support.DefaultErrorRenderingCallback;
import org.springmodules.xt.ajax.validation.support.DefaultSuccessRenderingCallback;
import org.springmodules.xt.ajax.validation.support.internal.ErrorsContainer;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.utils.RegDateEditor;

/**
 * @see org.springframework.web.servlet.mvc.EnhancedSimpleFormController
 *
 */
public abstract class AbstractEnhancedSimpleFormController extends EnhancedSimpleFormController implements AjaxHandler, MessageSourceAware {

	private static final Logger logger = Logger.getLogger(AbstractEnhancedSimpleFormController.class);

	private static final String ERRORS_PREFIX = DefaultValidationHandler.class.getName() + " - ";

	public static final String DEFAULT_ENCODING = "ISO-8859-1";

	private String ajaxResponseEncoding = DEFAULT_ENCODING;

	private ErrorRenderingCallback errorRenderingCallback = new DefaultErrorRenderingCallback();
	private SuccessRenderingCallback successRenderingCallback = new DefaultSuccessRenderingCallback();

	protected MessageSource messageSource;

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public AjaxResponse validate(AjaxSubmitEvent event) {
		AjaxResponse response = new AjaxResponseImpl(this.ajaxResponseEncoding);

		if (event.getValidationErrors() != null && event.getValidationErrors().hasErrors()) {
			this.removeOldErrors(event, response);
			this.putNewErrors(event, response);
		}
		else {
			AjaxAction[] successActions = this.successRenderingCallback.getSuccessActions(event);
			if (successActions != null && successActions.length > 0) {
				this.removeOldErrors(event, response);
				for (AjaxAction action : successActions) {
					response.addAction(action);
				}
			}
		}

		this.afterValidation(event, response);

		return response;
	}

	/**
	 * Set the encoding of the response produced by this handler. If not set, it defaults to ISO-8859-1.
	 */
	public void setAjaxResponseEncoding(String encoding) {
		this.ajaxResponseEncoding = encoding;
	}

	public void setErrorRenderingCallback(ErrorRenderingCallback errorRenderingCallback) {
		this.errorRenderingCallback = errorRenderingCallback;
	}

	public void setSuccessRenderingCallback(SuccessRenderingCallback successRenderingCallback) {
		this.successRenderingCallback = successRenderingCallback;
	}

	/**
	 * Return true if the given error must be rendered, otherwise it returns false.<br>
	 * By default, all errors are rendered.<br>
	 * Subclasses can override this to provide a different strategy.
	 */
	protected boolean rendersError(ObjectError error) {
		return true;
	}

	/**
	 * Get the {@link org.springmodules.xt.ajax.ElementMatcher} to use for selecting web page elements based on the ObjectError state.<br>
	 * By default, it returns a {@link org.springmodules.xt.ajax.action.matcher.WildcardMatcher} based on the error code.<br>
	 * Subclasses can override this to provide their own ElementMatcher strategy.
	 */
	protected ElementMatcher getElementMatcherForError(ObjectError error) {
		return new WildcardMatcher(error.getCode());
	}

	/**
	 * Override in subclasses to add additional actions to the AjaxResponse after standard validation processing.<br>
	 * The default implementation provided here just does nothing.
	 */
	protected void afterValidation(AjaxSubmitEvent event, AjaxResponse response) {
		// Do nothing by default ...
	}

	/**
	 * Initilise le Binder spring du formulaire. Ajoute l'Editor CustomDateEditor qui permet de mapper une date vers un objet Date dans le
	 * format par défaut de l'application. Ajoute egalement un CustomNumberEditor permettant de mapper vers un objet BigDecimal.
	 *
	 * Ajoute l'Editor CustomCollectionEditor qui permet de mapper une liste vers un objet List.
	 *
	 * Ajoute l'Editor CustomBooleanEditor qui permet de mapper une checkbox vers un boolean.
	 *
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
		Locale locale = request.getLocale();
		SimpleDateFormat sdf = new SimpleDateFormat(DateHelper.DATE_FORMAT_DISPLAY, locale);
		sdf.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
		NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(true);
		binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true));
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class));
		binder.registerCustomEditor(boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true));

	}

	/**
	 * Dynamic template method for handling ajax requests depending on the event id. <br>
	 * <br>
	 *
	 * @see AjaxHandler#handle(AjaxEvent )
	 */
	public AjaxResponse handle(AjaxEvent event) {
		if (event == null || event.getEventId() == null) {
			logger.error("Event and event id cannot be null.");
			throw new IllegalArgumentException("Event and event id cannot be null.");
		}

		String id = event.getEventId();
		AjaxResponse response = null;

// msi (30.09.2009) : tout ce code semble un peu overkill pour juste démarrer un job...
//		if (event instanceof AjaxSubmitEvent) {
//			response = validate((AjaxSubmitEvent) event);
//		}

		try {
			if (response != null) {
				Method m = this.getMatchingMethod(event, response);
				if (m != null) {
					if (logger.isTraceEnabled()) {
						logger.trace(new StringBuilder("Invoking method: ").append(m));
					}
					response = (AjaxResponse) m.invoke(this, event, response);
				}
				else {
					logger.error("You need to call the supports() method first!");
					throw new UnsupportedEventException("You need to call the supports() method first!");
				}
			}
			else {
				Method m = this.getMatchingMethod(event);
				if (m != null) {
					if (logger.isTraceEnabled()) {
						logger.trace(new StringBuilder("Invoking method: ").append(m));
					}
					response = (AjaxResponse) m.invoke(this, new Object[] {
						event
					});
				}
				else {
					logger.error("You need to call the supports() method first!");
					throw new UnsupportedEventException("You need to call the supports() method first!");
				}
			}
		}
		catch (IllegalAccessException ex) {
			logger.error(ex.getMessage(), ex);
			logger.error("Cannot handle the given event with id: " + id);
			throw new UnsupportedEventException("Cannot handle the given event with id: " + id, ex);
		}
		catch (InvocationTargetException ex) {
			logger.error(ex.getMessage(), ex);
			logger.error("Exception while handling the given event with id: " + id);
			throw new EventHandlingException("Exception while handling the given event with id: " + id, ex);
		}

		return response;
	}

	/**
	 * Supports the given event if the concrete class implements a method for handling it, that is, a method with the following signature:
	 * <br>
	 * <br>
	 * <i>public {@link AjaxResponse} eventId({@link AjaxEvent} )</i> <br>
	 * <br>
	 *
	 * @see AjaxHandler#supports(AjaxEvent )
	 */
	public boolean supports(AjaxEvent event) {
		String id = event.getEventId();

		if (id == null) {
			logger.error("Event id cannot be null.");
			throw new IllegalArgumentException("Event id cannot be null.");
		}

		Method m = this.getMatchingMethod(event);
		if (m != null) {
			if (logger.isTraceEnabled()) {
				logger.trace(new StringBuilder("Event supported by method: ").append(m));
			}
			return true;
		}
		else {
			return false;
		}
	}

	private Method getMatchingMethod(AjaxEvent event) {
		Class<?> eventType = this.getEventType(event);
		Method[] methods = this.getClass().getMethods();
		Method ret = null;
		for (Method method : methods) {
			if (method.getName().equals(event.getEventId()) && (method.getParameterTypes()[0].isAssignableFrom(eventType))) {
				ret = method;
				break;
			}
		}
		return ret;
	}

	private Method getMatchingMethod(AjaxEvent event, AjaxResponse response) {
		Class<?> eventType = this.getEventType(event);
		Method[] methods = this.getClass().getMethods();
		Method ret = null;
		for (Method method : methods) {
			if (method.getParameterTypes().length == 2) {
				if (method.getName().equals(event.getEventId()) && (method.getParameterTypes()[0].isAssignableFrom(eventType))
						&& (method.getParameterTypes()[1].isAssignableFrom(response.getClass()))) {
					ret = method;
					break;
				}
			}
		}
		return ret;
	}

	private Class<?> getEventType(AjaxEvent event) {
		Class<?>[] interfaces = event.getClass().getInterfaces();
		Class<?> ret = event.getClass();
		for (Class<?> intf : interfaces) {
			if (AjaxEvent.class.isAssignableFrom(intf)) {
				ret = intf;
				break;
			}
		}
		return ret;
	}

	private void removeOldErrors(AjaxSubmitEvent event, AjaxResponse response) {
		HttpServletRequest request = event.getHttpRequest();
		ErrorsContainer errorsContainer = (ErrorsContainer) request.getSession(true).getAttribute(this.getErrorsAttributeName(request));
		if (errorsContainer != null) {
			logger.debug("Found errors for URL: " + request.getRequestURL().toString());
			logger.debug("Removing old errors.");
			// Remove old errors from session:
			request.getSession(true).removeAttribute(this.getErrorsAttributeName(request));
			// Remove old errors from HTML:
			ObjectError[] errors = errorsContainer.getErrors();
			for (ObjectError error : errors) {
				if (this.rendersError(error)) {
					ElementMatcher matcher = this.getElementMatcherForError(error);
					RemoveContentAction removeAction = new RemoveContentAction(matcher);
					response.addAction(removeAction);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void putNewErrors(AjaxSubmitEvent event, AjaxResponse response) {
		List<ObjectError> errors = event.getValidationErrors().getAllErrors();
		HttpServletRequest request = event.getHttpRequest();
		Locale locale = LocaleContextHolder.getLocale(); // <- Get the current Locale, if any ...
		// Put new errors into http session for later retrieval:
		logger.debug("Putting errors in session for URL: " + request.getRequestURL().toString());
		request.getSession(true).setAttribute(this.getErrorsAttributeName(request),
				new ErrorsContainer(errors.toArray(new ObjectError[errors.size()])));
		// Put new errors into HTML:
		for (ObjectError error : errors) {
			if (this.rendersError(error)) {
				ElementMatcher matcher = this.getElementMatcherForError(error);
				Component renderingComponent = this.errorRenderingCallback.getErrorComponent(event, error, this.messageSource, locale);
				AppendContentAction appendAction = new AppendContentAction(matcher, renderingComponent);
				response.addAction(appendAction);
				// Get the actions to execute *after* rendering the component:
				AjaxAction[] renderingActions = this.errorRenderingCallback.getErrorActions(event, error);
				if (renderingActions != null) {
					for (AjaxAction renderingAction : renderingActions) {
						response.addAction(renderingAction);
					}
				}
			}
		}
	}

	private String getErrorsAttributeName(HttpServletRequest request) {
		return new StringBuilder(ERRORS_PREFIX).append(request.getRequestURL().toString()).toString();
	}
}
