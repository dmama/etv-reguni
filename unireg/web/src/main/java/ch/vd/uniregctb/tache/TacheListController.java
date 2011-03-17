package ch.vd.uniregctb.tache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.tache.manager.TacheListManager;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;
import ch.vd.uniregctb.tache.view.TacheListView;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * @author xcifde
 *
 */
public class TacheListController extends AbstractTacheController {

	protected static final Logger LOGGER = Logger.getLogger(TacheListController.class);

	private static final String TABLE_TACHE_ID = "tache";
	public static final String TACHE_CRITERIA_NAME = "tacheCriteria";
	public static final String RESULT_SIZE_NAME = "resultSize";
	public static final Integer PAGE_SIZE = new Integer(25);
	public static final String TACHE_LIST_ATTRIBUTE_NAME = "taches";

	private TacheListManager tacheListManager;

	public TacheListManager getTacheListManager() {
		return tacheListManager;
	}

	public void setTacheListManager(TacheListManager tacheListManager) {
		this.tacheListManager = tacheListManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final TracePoint tp = TracingManager.begin();

		final HttpSession session = request.getSession();

		TacheCriteriaView bean = (TacheCriteriaView) session.getAttribute(TACHE_CRITERIA_NAME);
		if (bean == null || isAppuiSurEffacer(request)) {
			bean = (TacheCriteriaView) super.formBackingObject(request);
			bean.setEtatTache(TypeEtatTache.EN_INSTANCE.toString());
			bean.setOfficeImpot(getDefaultOID());
			session.setAttribute(TACHE_CRITERIA_NAME, bean);
		}

		TracingManager.end(tp);
		return bean;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {

		final TracePoint tp = TracingManager.begin();
		final ModelAndView mav = super.showForm(request, response, errors, model);

		final HttpSession session = request.getSession();

		final TacheCriteriaView bean = (TacheCriteriaView) session.getAttribute(TACHE_CRITERIA_NAME);
		final boolean faireRecherche = bean != null && !bean.isEmpty() && !isAppuiSurEffacer(request);
		if (faireRecherche) {
			final WebParamPagination pagination = new WebParamPagination(request, TABLE_TACHE_ID, PAGE_SIZE);
			final List<TacheListView> tachesView = tacheListManager.find(bean, pagination);
			mav.addObject(TACHE_LIST_ATTRIBUTE_NAME, tachesView);
			mav.addObject(RESULT_SIZE_NAME, new Integer(tacheListManager.count(bean)));
		}
		else {
			mav.addObject(TACHE_LIST_ATTRIBUTE_NAME, new ArrayList<TacheListView>());
			mav.addObject(RESULT_SIZE_NAME, Integer.valueOf(0));
		}

		TracingManager.end(tp);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		final TracePoint tp = TracingManager.begin();

		final ModelAndView mav = super.onSubmit(request, response, command, errors);
		mav.setView(new RedirectView(getSuccessView()));

		final TacheCriteriaView bean = (TacheCriteriaView) command;
		final HttpSession session = request.getSession();
		session.setAttribute(TACHE_CRITERIA_NAME, bean);

		TracingManager.end(tp);

		TracingManager.outputMeasures(LOGGER);

		return mav;
	}
}
