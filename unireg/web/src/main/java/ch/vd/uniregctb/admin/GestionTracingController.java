package ch.vd.uniregctb.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tracing.TracingManager;

/**
 * Controller spring permettant la visualisation  des Traces
 *
 * @author KC</a>
 */
public class GestionTracingController extends AbstractSimpleFormController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(GestionTracingController.class);

	public static final String LIST_TRACES = "traces";

	public static final String GESTION_TRACING_NAME = "gestionTracing";



	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();
		GestionTracing bean = (GestionTracing) session.getAttribute(GESTION_TRACING_NAME);
		if (bean == null) {
			bean = (GestionTracing) super.formBackingObject(request);
			session.setAttribute(GESTION_TRACING_NAME, bean);
		}
		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
		throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		HttpSession session = request.getSession();
		//GestionTracing bean = (GestionTracing) session.getAttribute(GESTION_TRACING_NAME);

		mav.addObject(LIST_TRACES, session.getAttribute(LIST_TRACES));
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

		if (!SecurityProvider.isGranted(Role.ADMIN) && !SecurityProvider.isGranted(Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		GestionTracing bean = (GestionTracing) command;
		HttpSession session = request.getSession();
		ModelAndView mav = super.onSubmit(request, response, command, errors);
		if(bean.isGestionPerfActif()){
			LOGGER.info("case cochée : gestion trace active");
		}else {

			LOGGER.info("case non cochée : gestion trace non active");


		}

	/*	TracingManager.begin();
		TracePoint tp = TracingManager.begin("--------testTracePoints---------------");
		TracingManager.end(tp);
*/
		List<String> list = TracingManager.getMeasuresAsStringList("&nbsp;");
		List<String> listTrace = new ArrayList<String>();
		Iterator<String> it = list.iterator();

		while (it.hasNext()){

			String log = it.next();
			//tracing.setLogs(log);
			listTrace.add(log);

		}
		session.setAttribute(LIST_TRACES, listTrace);

		mav.setView(new RedirectView(getSuccessView()));
		return mav;
	}



}

