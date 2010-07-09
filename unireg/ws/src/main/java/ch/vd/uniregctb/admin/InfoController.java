package ch.vd.uniregctb.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.common.HtmlHelper;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.utils.UniregProperties;

/**
 * Ce contrôleur affiche des informations sur le status de l'application. Il est donc en read-only.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class InfoController extends ParameterizableViewController {

	// private static final Logger LOGGER = Logger.getLogger(InfoController.class);

	private UniregProperties uniregProperties;
	private ServiceCivilService serviceCivilRaw;
	private ServiceInfrastructureService serviceInfraRaw;
	private ServiceSecuriteService serviceSecuriteRaw;
	private CacheManager cacheManager;
	private StatsService statsService;

	public void setUniregProperties(UniregProperties uniregProperties) {
		this.uniregProperties = uniregProperties;
	}

	public void setServiceCivilRaw(ServiceCivilService serviceCivilRaw) {
		this.serviceCivilRaw = serviceCivilRaw;
	}

	public void setServiceInfraRaw(ServiceInfrastructureService serviceInfraRaw) {
		this.serviceInfraRaw = serviceInfraRaw;
	}

	public void setServiceSecuriteRaw(ServiceSecuriteService serviceSecuriteRaw) {
		this.serviceSecuriteRaw = serviceSecuriteRaw;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mav = super.handleRequestInternal(request, response);

		mav.addObject("cacheStatus", getCacheStatus());
		fillServiceCivilInfo(mav);
		fillServiceInfraInfo(mav);
		fillServiceSecuriteInfo(mav);
		fillStatsServiceInfo(mav);

		mav.addObject("log4jConfig", getLog4jConfig());
		mav.addObject("logFilename", getLogFilename());
		mav.addObject("extProps", getExtProps());

		return mav;
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
		try {
			Individu individu = serviceCivilRaw.getIndividu(611836, 2400); // Francis Perroset
			Assert.isEqual(611836L, individu.getNoTechnique());
			mav.addObject("serviceCivilStatus", "OK");
		}
		catch (Exception e) {
			String callstack = ExceptionUtils.extractCallStack(e);
			mav.addObject("serviceCivilStatus", "NOK");
			mav.addObject("serviceCivilException",HtmlHelper.renderMultilines(callstack));
		}
	}

	private void fillServiceInfraInfo(ModelAndView mav) {
		try {
			CollectiviteAdministrative aci = serviceInfraRaw.getCollectivite(ServiceInfrastructureService.noACI);
			Assert.isEqual(ServiceInfrastructureService.noACI, aci.getNoColAdm());
			mav.addObject("serviceInfraStatus", "OK");
		}
		catch (Exception e) {
			String callstack = ExceptionUtils.extractCallStack(e);
			mav.addObject("serviceInfraStatus", "NOK");
			mav.addObject("serviceInfraException",HtmlHelper.renderMultilines(callstack));
		}
	}

	private void fillServiceSecuriteInfo(ModelAndView mav) {
		try {
			Operateur op = serviceSecuriteRaw.getOperateur("zaiptf");
			Assert.isTrue("zaiptf".equalsIgnoreCase(op.getCode()));
			mav.addObject("serviceSecuriteStatus", "OK");
		}
		catch (Exception e) {
			String callstack = ExceptionUtils.extractCallStack(e);
			mav.addObject("serviceSecuriteStatus", "NOK");
			mav.addObject("serviceSecuriteException",HtmlHelper.renderMultilines(callstack));
		}
	}

	private void fillStatsServiceInfo(ModelAndView mav) {
		final String stats = statsService.buildStats();
		mav.addObject("stats", HtmlHelper.renderMultilines(stats));
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
}