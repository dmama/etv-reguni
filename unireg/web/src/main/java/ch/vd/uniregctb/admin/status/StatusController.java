package ch.vd.uniregctb.admin.status;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Statistics;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.checker.ServiceChecker;
import ch.vd.uniregctb.common.HtmlHelper;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Ce contrôleur affiche des informations sur le status de l'application. Il est donc en read-only.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class StatusController {

	// private static final Logger LOGGER = Logger.getLogger(InfoController.class);

	private TiersDAO dao;
	private GlobalTiersSearcher globalSearcher;
	private CacheManager cacheManager;
	private StatsService statsService;
	private UniregModeHelper modeHelper;

	private ServiceChecker serviceCivilChecker;
	private ServiceChecker serviceInfraChecker;
	private ServiceChecker serviceSecuriteChecker;
	private ServiceChecker serviceBVRChecker;
	private ServiceChecker serviceEFactureChecker;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setGlobalSearcher(GlobalTiersSearcher globalSearcher) {
		this.globalSearcher = globalSearcher;
	}

	public void setTiersDAO(TiersDAO dao) {
		this.dao = dao;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setModeHelper(UniregModeHelper modeHelper) {
		this.modeHelper = modeHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilChecker(ServiceChecker serviceCivilChecker) {
		this.serviceCivilChecker = serviceCivilChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfraChecker(ServiceChecker serviceInfraChecker) {
		this.serviceInfraChecker = serviceInfraChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteChecker(ServiceChecker serviceSecuriteChecker) {
		this.serviceSecuriteChecker = serviceSecuriteChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceBVRChecker(ServiceChecker serviceBVRChecker) {
		this.serviceBVRChecker = serviceBVRChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceEFactureChecker(ServiceChecker serviceEFactureChecker) {
		this.serviceEFactureChecker = serviceEFactureChecker;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/admin/status.do", method = RequestMethod.GET)
	public String status(Model model) {
		model.addAttribute("serviceCivilName", modeHelper.getServiceCivilSource());
		model.addAttribute("indexCount", getIndexCount());
		model.addAttribute("tiersCount", getTiersCount());
		model.addAttribute("cacheStatus", getCacheStatus());
		model.addAttribute("serviceStats", getServiceStats());
		return "/admin/status";
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/civil.do", method = RequestMethod.GET)
	public ServiceStatusView civilStatus() {
		return new ServiceStatusView("serviceCivil", serviceCivilChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/infra.do", method = RequestMethod.GET)
	public ServiceStatusView infraStatus() {
		return new ServiceStatusView("serviceInfra", serviceInfraChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/securite.do", method = RequestMethod.GET)
	public ServiceStatusView securiteStatus() {
		return new ServiceStatusView("serviceSecurite", serviceSecuriteChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/bvr.do", method = RequestMethod.GET)
	public ServiceStatusView bvrPlusStatus() {
		return new ServiceStatusView("serviceBVR", serviceBVRChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/efacture.do", method = RequestMethod.GET)
	public ServiceStatusView efactureStatus() {
		return new ServiceStatusView("serviceEFacture", serviceEFactureChecker);
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
		String tiersCount;
		try {
			tiersCount = String.valueOf(dao.getCount(Tiers.class));
		}
		catch (Exception e) {
			tiersCount = e.getMessage();
		}
		return tiersCount;
	}

	private String getServiceStats() {
		final String stats = statsService.buildStats();
		return HtmlHelper.renderMultilines(stats);
	}

	private String getCacheStatus() {
		String status = "";
		try {
			String[] names = cacheManager.getCacheNames();
			for (String name : names) {
				Cache cache = cacheManager.getCache(name);
				Statistics stats = cache.getStatistics();
				status += stats.toString() + '\n';
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
