package ch.vd.uniregctb.common;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.utils.RegDateEditor;

/**
 * @see org.springframework.web.servlet.mvc.SimpleFormController
 *
 */
public abstract class AbstractSimpleFormController extends CommonSimpleFormController {

	public final static String PARAMETER_MODIFIER = "__MODIFIER__";

	public final static String PARAMETER_TARGET = "__TARGET__";
	public final static String PARAMETER_EVENT_ARGUMENT = "__EVENT_ARGUMENT__";
	public final static String PARAMETER_URL_RETOUR = "__URL_RETOUR__";

	private boolean modified = false;

	private String target;
	private String eventArgument;
	private String urlRetour;

	protected AbstractSimpleFormController() {
		// On change le logger de la classe Spring 'ApplicationObjectSupport', de manière à pouvoir régler le niveau de log par package.
		try {
			Field field = ApplicationObjectSupport.class.getDeclaredField("logger");
			field.setAccessible(true);
			field.set(this, LogFactory.getLog(ApplicationObjectSupport.class));
		}
		catch (Exception e) {
			// tant pis, on aura essayé
		}
	}

	/**
	 * Initilise le Binder spring du formulaire. Ajoute l'Editor CustomDateEditor qui permet de mapper une date vers un objet Date dans le
	 * format par d�faut de l'application. Ajoute egalement un CustomNumberEditor permettant de mapper vers un objet BigDecimal.
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
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));

	}

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String modifier = request.getParameter(PARAMETER_MODIFIER);
		target = null;
		eventArgument = null;
		urlRetour = null;

		if (this.isFormSubmission(request)) {
			target = StringUtils.trimToNull(request.getParameter(PARAMETER_TARGET));
			eventArgument = StringUtils.trimToNull(request.getParameter(PARAMETER_EVENT_ARGUMENT));
			urlRetour = StringUtils.trimToNull(request.getParameter(PARAMETER_URL_RETOUR));
		}

		modified = StringUtils.isNotBlank(modifier) && Boolean.parseBoolean(modifier);
		
		ModelAndView view = super.handleRequest(request, response);
		request.setAttribute(PARAMETER_MODIFIER, modified);
		return view;
	}

	/**
	 * Récupère et interprète le numéro de tiers dans le paramètre spécifié.
	 *
	 * @param request
	 *            la requête http possèdant (ou non) un paramètre
	 * @param parameterName
	 *            le nom du paramètre contenant le numéro à extraire
	 * @return le numéro de tiers extrait, ou <b<>null</b> si le paramètre n'est pas renseigné, ou la valeur ne peut pas être interprétée
	 *         comme un long.
	 */
	protected Long extractLongParam(HttpServletRequest request, String parameterName) {
		String string = request.getParameter(parameterName);
		Long id = null;
		if (string != null) {
			try {
				id = Long.valueOf(string);
			}
			catch (NumberFormatException e) {
				id = null;
			}
		}
		return id;
	}

	protected static Boolean extractBooleanParam(HttpServletRequest request, String parameterName) {
		Boolean forPrint = null;
		String string = request.getParameter(parameterName);
		if (string != null) {
			try {
				forPrint = Boolean.valueOf(string);
			}
			catch (NumberFormatException e) {
				forPrint = false;
			}
		}
		return forPrint;
	}

	/**
	 * Vérifie que l'opérateur courant possède les droits d'accès en lecture et écriture sur le <b>dossier</b> du tiers spécifié.
	 * <p>
	 * <b>Attention !</b> Cette méthode ne vérifie pas les droits IFOSec. Un ou plusieurs appels à la méthode
	 * {@link SecurityProvider#isGranted(ch.vd.uniregctb.security.Role)} sont nécessaires en complément.
	 *
	 * @param tiersId
	 *            le tiers dont on veut vérifier les droits d'accès au dossier.
	 * @throws ObjectNotFoundException
	 *             si le tiers spécifié n'existe pas
	 * @throws AccessDeniedException
	 *             si l'opérateur ne possède pas les droits d'accès suffisants.
	 */
	protected static void checkAccesDossierEnLecture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {
		ControllerUtils.checkAccesDossierEnLecture(tiersId);
	}

	/**
	 * Vérifie que l'opérateur courant possède les droits d'accès en lecture et écriture sur le <b>dossier</b> du tiers spécifié.
	 * <p>
	 * <b>Attention !</b> Cette méthode ne vérifie pas les droits IFOSec. Un ou plusieurs appels à la méthode
	 * {@link SecurityProvider#isGranted(ch.vd.uniregctb.security.Role)} sont nécessaires en complément.
	 *
	 * @param tiersId
	 *            le tiers dont on veut vérifier les droits d'accès au dossier.
	 * @throws ObjectNotFoundException
	 *             si le tiers spécifié n'existe pas
	 * @throws AccessDeniedException
	 *             si l'opérateur ne possède pas les droits d'accès suffisants.
	 */
	protected static void checkAccesDossierEnEcriture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {
		ControllerUtils.checkAccesDossierEnEcriture(tiersId);
	}

	/**
	 * Version spécialisée de {@link SimpleFormController#processFormSubmission} qui catche les erreurs de validation, renseigne les erreurs
	 * détectées et réaffiche le formulaire automatiquement.
	 */
	@Override
	protected ModelAndView processFormSubmission(HttpServletRequest request, HttpServletResponse response, Object command,
			BindException errors) throws Exception {

		if (errors.hasErrors()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Data binding errors: " + errors.getErrorCount());
			}
			return showForm(request, response, errors);
		}
		else if (isFormChangeRequest(request, command)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Detected form change request -> routing request to onFormChange");
			}
			onFormChange(request, response, command, errors);
			return showForm(request, response, errors);
		}
		else {
			try {
				if (logger.isTraceEnabled()) {
					logger.trace("No errors -> processing submit");
				}
				return onSubmit(request, response, command, errors);
			}
			catch (ActionException e) {
				logger.debug("Action exception catched -> redisplaying showForm : " + e.getMessage());
				for (String s : e.getErrors()) {
					errors.reject("global.error.msg", s);
				}
				return showForm(request, response, errors);
			}
			catch (ValidationException e) {
				logger.debug("Validation exception catched -> redisplaying showForm : " + e.getMessage());
				for (ValidationMessage s : e.getErrors()) {
					errors.reject("global.error.msg", s.getMessage());
				}
				return showForm(request, response, errors);
			}
			catch (ObjectNotFoundException  e) {
				logger.debug("ObjectNotFound exception catched -> redisplaying showForm : " + e.getMessage());
				errors.reject("global.error.msg", e.getMessage());
				return showForm(request, response, errors);
			}
			catch (EvenementCivilException e){
				logger.debug("EvenementCivilHandler exception catched -> redisplaying showForm : " + e.getMessage());
				errors.reject("global.error.msg", e.getMessage());
				return showForm(request, response, errors);
			}
		}
	}

	protected static Long getLongParam(HttpServletRequest request, String paramName) {
		final String longAsString = request.getParameter(paramName);
		if (StringUtils.isBlank(longAsString)) {
			return null;
		}
		return Long.valueOf(longAsString);
	}

	/**
	 * @return the modified
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * @param modified
	 *            the modified to set
	 */
	public void setModified(boolean modified) {
		this.modified = modified;
	}

	/**
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @return the argument
	 */
	public String getEventArgument() {
		return eventArgument;
	}

	/**
	 * @return the argument
	 */
	public Long getLongEventArgument() {
		if (StringUtils.isBlank(eventArgument)) {
			return null;
		}
		else {
			return Long.valueOf(eventArgument);
		}
	}

	public String getUrlRetour() {
		return urlRetour;
	}
}
