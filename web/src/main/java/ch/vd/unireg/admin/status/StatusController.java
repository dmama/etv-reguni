package ch.vd.unireg.admin.status;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.statistics.StatisticsGateway;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.shared.statusmanager.StatusManager;
import ch.vd.unireg.checker.ServiceBVRChecker;
import ch.vd.unireg.checker.ServiceCivilChecker;
import ch.vd.unireg.checker.ServiceEFactureChecker;
import ch.vd.unireg.checker.ServiceEntrepriseChecker;
import ch.vd.unireg.checker.ServiceInfraChecker;
import ch.vd.unireg.checker.ServiceSecuriteChecker;
import ch.vd.unireg.common.HtmlHelper;
import ch.vd.unireg.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.indexer.messageidentification.GlobalMessageIdentificationSearcher;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

/**
 * Ce contrôleur affiche des informations sur le status de l'application. Il est donc en read-only.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class StatusController {

	// private static final Logger LOGGER = LoggerFactory.getLogger(InfoController.class);

	private TiersDAO tiersDAO;
	private GlobalTiersSearcher tiersSearcher;

	private IdentCtbDAO identCtbDAO;
	private GlobalMessageIdentificationSearcher identSearcher;

	private CacheManager cacheManager;
	private StatsService statsService;
	private StatusManager statusManager;

	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}

	public void setTiersDAO(TiersDAO dao) {
		this.tiersDAO = dao;
	}

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setIdentSearcher(GlobalMessageIdentificationSearcher identSearcher) {
		this.identSearcher = identSearcher;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setStatusManager(StatusManager statusManager) {
		this.statusManager = statusManager;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/admin/status.do", method = RequestMethod.GET)
	public String status(Model model) {
		model.addAttribute("tiersIndexCount", getTiersIndexCount());
		model.addAttribute("tiersCount", getTiersCount());
		model.addAttribute("identIndexCount", getIdentificationIndexCount());
		model.addAttribute("identCount", getIdentificationCount());
		model.addAttribute("cacheStatus", getCacheStatus());
		model.addAttribute("serviceStats", getServiceStats());
		return "/admin/status";
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/civil.do", method = RequestMethod.GET)
	public ServiceStatusView civilStatus() {
		return new ServiceStatusView(statusManager, ServiceCivilChecker.NAME);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/entreprise.do", method = RequestMethod.GET)
	public ServiceStatusView entrepriseStatus() {
		return new ServiceStatusView(statusManager, ServiceEntrepriseChecker.NAME);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/infra.do", method = RequestMethod.GET)
	public ServiceStatusView infraStatus() {
		return new ServiceStatusView(statusManager, ServiceInfraChecker.NAME);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/securite.do", method = RequestMethod.GET)
	public ServiceStatusView securiteStatus() {
		return new ServiceStatusView(statusManager, ServiceSecuriteChecker.NAME);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/bvr.do", method = RequestMethod.GET)
	public ServiceStatusView bvrPlusStatus() {
		return new ServiceStatusView(statusManager, ServiceBVRChecker.NAME);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/efacture.do", method = RequestMethod.GET)
	public ServiceStatusView efactureStatus() {
		return new ServiceStatusView(statusManager, ServiceEFactureChecker.NAME);
	}

	private String getTiersIndexCount() {
		String indexCount;
		try {
			indexCount = String.valueOf(tiersSearcher.getApproxDocCount());
		}
		catch (Exception e) {
			indexCount = e.getMessage();
		}
		return indexCount;
	}

	private String getTiersCount() {
		String tiersCount;
		try {
			tiersCount = String.valueOf(tiersDAO.getCount(Tiers.class));
		}
		catch (Exception e) {
			tiersCount = e.getMessage();
		}
		return tiersCount;
	}

	private String getIdentificationIndexCount() {
		String indexCount;
		try {
			indexCount = String.valueOf(identSearcher.getApproxDocCount());
		}
		catch (Exception e) {
			indexCount = e.getMessage();
		}
		return indexCount;
	}

	private String getIdentificationCount() {
		String tiersCount;
		try {
			tiersCount = String.valueOf(identCtbDAO.getCount(IdentificationContribuable.class));
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
		try {
			final StringBuilder status = new StringBuilder();
			String[] names = cacheManager.getCacheNames();
			for (String name : names) {
				final Cache cache = cacheManager.getCache(name);
				final StatisticsGateway stats = cache.getStatistics();
				final String line = "name = " + name +
						" cacheHits = " + stats.cacheHitCount() +
						" onDiskHits = " + stats.localDiskHitCount() +
						" inMemoryHits = " + stats.localHeapHitCount() +
						" misses = " + stats.cacheMissCount() +
						" size = " + stats.getSize() +
						" evictionCount = " + stats.cacheEvictedCount();
				status.append(line).append('\n');
			}
			return HtmlHelper.renderMultilines(status.toString());
		}
		catch (Exception e) {
			String status = "NOK\n" + ExceptionUtils.getStackTrace(e);
			status = HtmlHelper.renderMultilines(status);
			return status;
		}
	}
}
