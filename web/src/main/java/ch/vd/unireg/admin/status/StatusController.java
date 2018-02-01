package ch.vd.unireg.admin.status;

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
import ch.vd.shared.statusmanager.StatusChecker;
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

	private StatusChecker serviceCivilChecker;
	private StatusChecker serviceOrganisationChecker;
	private StatusChecker serviceInfraChecker;
	private StatusChecker serviceSecuriteChecker;
	private StatusChecker serviceBVRChecker;
	private StatusChecker serviceEFactureChecker;

	@SuppressWarnings({"UnusedDeclaration"})
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilChecker(StatusChecker serviceCivilChecker) {
		this.serviceCivilChecker = serviceCivilChecker;
	}

	public void setServiceOrganisationChecker(StatusChecker serviceOrganisationChecker) {
		this.serviceOrganisationChecker = serviceOrganisationChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfraChecker(StatusChecker serviceInfraChecker) {
		this.serviceInfraChecker = serviceInfraChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteChecker(StatusChecker serviceSecuriteChecker) {
		this.serviceSecuriteChecker = serviceSecuriteChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceBVRChecker(StatusChecker serviceBVRChecker) {
		this.serviceBVRChecker = serviceBVRChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceEFactureChecker(StatusChecker serviceEFactureChecker) {
		this.serviceEFactureChecker = serviceEFactureChecker;
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
		return new ServiceStatusView(serviceCivilChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/organisation.do", method = RequestMethod.GET)
	public ServiceStatusView organisationStatus() {
		return new ServiceStatusView(serviceOrganisationChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/infra.do", method = RequestMethod.GET)
	public ServiceStatusView infraStatus() {
		return new ServiceStatusView(serviceInfraChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/securite.do", method = RequestMethod.GET)
	public ServiceStatusView securiteStatus() {
		return new ServiceStatusView(serviceSecuriteChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/bvr.do", method = RequestMethod.GET)
	public ServiceStatusView bvrPlusStatus() {
		return new ServiceStatusView(serviceBVRChecker);
	}

	@ResponseBody
	@RequestMapping(value = "/admin/status/efacture.do", method = RequestMethod.GET)
	public ServiceStatusView efactureStatus() {
		return new ServiceStatusView(serviceEFactureChecker);
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
