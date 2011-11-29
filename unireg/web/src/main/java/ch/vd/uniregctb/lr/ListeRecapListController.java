package ch.vd.uniregctb.lr;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.declaration.ListeRecapCriteria;
import ch.vd.uniregctb.lr.manager.ListeRecapListManager;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

public class ListeRecapListController extends AbstractListeRecapController {

	protected static final Logger LOGGER = Logger.getLogger(ListeRecapListController.class);

	private ListeRecapListManager lrListManager;

	private static final String ACTION_PARAMETER_NAME = "action";
	private static final String EFFACER_PARAMETER_VALUE = "effacer";

	public static final String RESULT_SIZE_NAME = "resultSize";


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		TracePoint tp = TracingManager.begin();

		HttpSession session = request.getSession();
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		ListeRecapCriteria bean = new ListeRecapCriteria();

		if(!SecurityProvider.isGranted(Role.LR)){
			TracingManager.end(tp);
			throw new AccessDeniedException("vous n'avez pas le droit d'accéder aux listes récapitulatives pour l'application Unireg");
		}
		if((buttonEffacer != null) && (buttonEffacer.equals(EFFACER_PARAMETER_VALUE))) {
			removeModuleFromSession(request, LR_CRITERIA_NAME);
		} else {
			bean = (ListeRecapCriteria) session.getAttribute(LR_CRITERIA_NAME);
		 	if (bean == null) {
		 		bean = (ListeRecapCriteria) super.formBackingObject(request);
		 		session.setAttribute(LR_CRITERIA_NAME, bean);
		 	}
		}

		TracingManager.end(tp);
		return bean;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {

		final TracePoint tp = TracingManager.begin();
		final String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		final ModelAndView mav  =  super.showForm(request, response, errors, model);
		mav.addObject(RESULT_SIZE_NAME, 0);

		if(buttonEffacer == null) {
			if ((errors == null) || (errors.getAllErrors() == null) ||  (errors.getAllErrors().isEmpty())) {
				final HttpSession session = request.getSession();

				final ListeRecapCriteria bean = (ListeRecapCriteria) session.getAttribute(LR_CRITERIA_NAME);
				if (bean != null && !bean.isEmpty()) {
					final WebParamPagination pagination = new WebParamPagination(request, "lr", 25);
					final List<ListeRecapDetailView> lrsView = lrListManager.find(bean, pagination);
					mav.addObject(LR_LIST_ATTRIBUTE_NAME, lrsView);
					mav.addObject(RESULT_SIZE_NAME, lrListManager.count(bean));
				}
			}
			else
			{
				removeModuleFromSession(request, LR_CRITERIA_NAME);
			}
		}
		TracingManager.end(tp);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {

		super.onBindAndValidate(request, command, errors);

	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		TracePoint tp = TracingManager.begin();

		ModelAndView mav = super.onSubmit(request, response, command, errors);

		mav.setView(new RedirectView(getSuccessView()));

		ListeRecapCriteria bean = (ListeRecapCriteria) command;
		HttpSession session = request.getSession();
		session.setAttribute(LR_CRITERIA_NAME, bean);

		TracingManager.end(tp);

		TracingManager.outputMeasures(LOGGER);

		return mav;
	}

	/**
	 * Removes the mapping for this module.
	 * @param	request	HttpRequest
	 * @param	module	Name of the specific module
	 */
	public static void removeModuleFromSession(HttpServletRequest request, String module) {
		HttpSession session = request.getSession(true);
		session.removeAttribute(module);

	}

	public void setLrListManager(ListeRecapListManager lrListManager) {
		this.lrListManager = lrListManager;
	}

}
