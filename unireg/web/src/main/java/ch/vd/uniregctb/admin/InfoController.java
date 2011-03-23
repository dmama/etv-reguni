package ch.vd.uniregctb.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxEvent;
import org.springmodules.xt.ajax.AjaxHandler;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.SimpleText;
import org.springmodules.xt.ajax.support.UnsupportedEventException;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.checker.ServiceChecker;
import ch.vd.uniregctb.checker.Status;
import ch.vd.uniregctb.common.HtmlHelper;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.log.RollingInMemoryAppender;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.UniregProperties;

/**
 * Ce contrôleur affiche des informations sur le status de l'application. Il est donc en read-only.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class InfoController extends ParameterizableViewController implements AjaxHandler, InitializingBean {

	// private static final Logger LOGGER = Logger.getLogger(InfoController.class);

	private TiersDAO dao;
	private GlobalTiersSearcher globalSearcher;
	private UniregProperties uniregProperties;
	private CacheManager cacheManager;
	private GestionBatchController batchController;
	private StatsService statsService;

	private ServiceChecker serviceCivilChecker;
	private ServiceChecker serviceHostInfraChecker;
	private ServiceChecker serviceFidorChecker;
	private ServiceChecker serviceSecuriteChecker;
	private ServiceChecker serviceBVRChecker;

	private PlatformTransactionManager transactionManager;

	private RollingInMemoryAppender inMemoryAppender;

	public void setGlobalSearcher(GlobalTiersSearcher globalSearcher) {
		this.globalSearcher = globalSearcher;
	}

	public void setTiersDAO(TiersDAO dao) {
		this.dao = dao;
	}

	public void setUniregProperties(UniregProperties uniregProperties) {
		this.uniregProperties = uniregProperties;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void setBatchController(GestionBatchController batchController) {
		this.batchController = batchController;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setServiceCivilChecker(ServiceChecker serviceCivilChecker) {
		this.serviceCivilChecker = serviceCivilChecker;
	}

	public void setServiceHostInfraChecker(ServiceChecker serviceHostInfraChecker) {
		this.serviceHostInfraChecker = serviceHostInfraChecker;
	}

	public void setServiceFidorChecker(ServiceChecker serviceFidorChecker) {
		this.serviceFidorChecker = serviceFidorChecker;
	}

	public void setServiceSecuriteChecker(ServiceChecker serviceSecuriteChecker) {
		this.serviceSecuriteChecker = serviceSecuriteChecker;
	}

	public void setServiceBVRChecker(ServiceChecker serviceBVRChecker) {
		this.serviceBVRChecker = serviceBVRChecker;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mav = super.handleRequestInternal(request, response);

		mav.addObject("indexCount", getIndexCount());
		mav.addObject("tiersCount", getTiersCount());
		mav.addObject("cacheStatus", getCacheStatus());
		fillServiceCivilInfo(mav);
		fillServiceHostInfraInfo(mav);
		fillServiceFidorInfo(mav);
		fillServiceSecuriteInfo(mav);
		fillStatsServiceInfo(mav);
		fillBvrClientInfo(mav);


		if (SecurityProvider.isGranted(Role.ADMIN) || SecurityProvider.isGranted(Role.TESTER)) {
			// les informations ci-dessous peuvent contenir des informations sensibles (url des serveurs, etc...)
			mav.addObject("log4jConfig", getLog4jConfig());
			mav.addObject("logFilename", getLogFilename());
			mav.addObject("tailLog", getTailLog());
			mav.addObject("extProps", getExtProps());
		}

		return mav;
	}

	private String getIndexCount() {
		String indexCount;
		try {
			indexCount = String.valueOf(globalSearcher.getApproxDocCount());
		}
		catch (Exception e) {
			indexCount = e.getMessage();
		}
		return indexCount;
	}

	private String getTiersCount() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return (String) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				String tiersCount;
				try {
					tiersCount = String.valueOf(dao.getCount(Tiers.class));
				}
				catch (Exception e) {
					tiersCount = e.getMessage();
				}
				return tiersCount;
			}
		});
	}

	private String getExtProps() {
		String extProps;
		try {
			extProps = uniregProperties.dumpProps(true);
			extProps = HtmlHelper.renderMultilines(extProps);
		}
		catch (Exception e) {
			extProps = e.getMessage();
		}
		return extProps;
	}

	private String getLogFilename() {
		String logFile;
		try {
			FileAppender appender = (FileAppender) Logger.getRootLogger().getAppender("LOGFILE");
			logFile = appender.getFile();
		}
		catch (Exception e) {
			logFile = e.getMessage();
		}
		return logFile;
	}

	private String getTailLog() {
		if (inMemoryAppender == null) {
			return "<indisponible>";
		}
		else {
			String log = inMemoryAppender.getLogBuffer();
			log = HtmlHelper.renderMultilines(log);
			return log;
		}
	}

	private String getLog4jConfig() {
		String log4j;
		try {
			log4j = getServletContext().getInitParameter("log4jConfigLocation");
		}
		catch (Exception e) {
			log4j = e.getMessage();
		}
		return log4j;
	}

	private void fillServiceCivilInfo(ModelAndView mav) {
		final Status status = serviceCivilChecker.getStatus();
		mav.addObject("serviceCivilStatus", status.name());
		if (status == Status.KO) {
			mav.addObject("serviceCivilException", HtmlHelper.renderMultilines(serviceCivilChecker.getStatusDetails()));
		}
	}

	private void fillServiceHostInfraInfo(ModelAndView mav) {
		final Status status = serviceHostInfraChecker.getStatus();
		mav.addObject("serviceInfraStatus", status.name());
		if (status == Status.KO) {
			mav.addObject("serviceInfraException", HtmlHelper.renderMultilines(serviceHostInfraChecker.getStatusDetails()));
		}
	}

	private void fillServiceFidorInfo(ModelAndView mav) {
		final Status status = serviceFidorChecker.getStatus();
		mav.addObject("serviceFidorStatus", status.name());
		if (status == Status.KO) {
			mav.addObject("serviceFidorException", HtmlHelper.renderMultilines(serviceFidorChecker.getStatusDetails()));
		}
	}

	private void fillServiceSecuriteInfo(ModelAndView mav) {
		final Status status = serviceSecuriteChecker.getStatus();
		mav.addObject("serviceSecuriteStatus", status.name());
		if (status == Status.KO) {
			mav.addObject("serviceSecuriteException", HtmlHelper.renderMultilines(serviceSecuriteChecker.getStatusDetails()));
		}
	}

	private void fillStatsServiceInfo(ModelAndView mav) {
		final String stats = statsService.buildStats();
		mav.addObject("stats", HtmlHelper.renderMultilines(stats));
	}

	private void fillBvrClientInfo(ModelAndView mav) {
		final Status status = serviceBVRChecker.getStatus();
		String s = status.name();
		if (status == Status.KO) {
			s += " \n\n" + HtmlHelper.renderMultilines(serviceBVRChecker.getStatusDetails());
		}
		mav.addObject("bvrPlusStatus", s);
	}

	private String getCacheStatus() {
		String status = "";
		try {
			String[] names = cacheManager.getCacheNames();
			for (String name : names) {
				Cache cache = cacheManager.getCache(name);
				Statistics stats = cache.getStatistics();
				status += stats.toString() + "\n";
			}
			status = HtmlHelper.renderMultilines(status);
		}
		catch (Exception e) {
			String callstack = ExceptionUtils.extractCallStack(e);
			status = "NOK\n" + callstack;
			status = HtmlHelper.renderMultilines(status);
		}
		return status;
	}

	/**
	 * Cette méthode est appelée par une requête Ajax pour mettre-à-jour la liste des jobs actifs dans l'écran d'état de l'application.
	 */
	public AjaxResponse loadJobActif(AjaxActionEvent event) {
		// on affiche la table en read-only (sans les boutons pour interrompre les batches)
		return batchController.doLoadJobActif(event, true);
	}

	public AjaxResponse updateTailLog(AjaxActionEvent event) {
		AjaxResponse response = new AjaxResponseImpl();
		List<Component> components = new ArrayList<Component>(1);
		components.add(new SimpleText(getTailLog()));
		response.addAction(new ReplaceContentAction("logdiv", components));
		return response;
	}

	public AjaxResponse handle(AjaxEvent event) {
		if ("loadJobActif".equals(event.getEventId())) {
			return loadJobActif((AjaxActionEvent) event);
		}
		else if ("updateTailLog".equals(event.getEventId())) {
			return updateTailLog((AjaxActionEvent) event);
		}
		logger.error("You need to call the supports() method first!");
		throw new UnsupportedEventException("You need to call the supports() method first!");
	}

	public boolean supports(AjaxEvent event) {
		if (!(event instanceof AjaxActionEvent)) {
			return false;
		}
		final String id = event.getEventId();
		return ("loadJobActif".equals(id) || ("updateTailLog".equals(id)));
	}

	public void afterPropertiesSet() throws Exception {
		inMemoryAppender = (RollingInMemoryAppender) Logger.getRootLogger().getAppender("IN_MEMORY");
	}
}
