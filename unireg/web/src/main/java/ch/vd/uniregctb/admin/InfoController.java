package ch.vd.uniregctb.admin;

import ch.vd.registre.base.utils.Assert;
import ch.vd.securite.model.Operateur;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;
import ch.vd.uniregctb.common.ExceptionUtils;
import ch.vd.uniregctb.common.HtmlHelper;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.log.RollingInMemoryAppender;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.UniregProperties;
import ch.vd.uniregctb.webservice.sipf.BVRPlusClient;
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
import org.springmodules.xt.ajax.*;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.SimpleText;
import org.springmodules.xt.ajax.support.UnsupportedEventException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
	private ServiceCivilService serviceCivilRaw;
	private ServiceInfrastructureService serviceInfraRaw;
	private ServiceSecuriteService serviceSecuriteRaw;
	private CacheManager cacheManager;
	private GestionBatchController batchController;
	private StatsService statsService;
	private BVRPlusClient bvrClient;

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

	public void setBatchController(GestionBatchController batchController) {
		this.batchController = batchController;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setBvrClient(BVRPlusClient bvrClient) {
		this.bvrClient = bvrClient;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mav = super.handleRequestInternal(request, response);

		mav.addObject("indexCount", getIndexCount());
		mav.addObject("tiersCount", getTiersCount());
		mav.addObject("cacheStatus", getCacheStatus());
		fillServiceCivilInfo(mav);
		fillServiceInfraInfo(mav);
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

	private void fillBvrClientInfo(ModelAndView mav) {
		try {
			final BvrDemande demande = new BvrDemande();
			demande.setNdc("0");
			demande.setAnneeTaxation(BigInteger.valueOf(2009));
			demande.setTypeDebiteurIS("REGULIER");
			
			final BvrReponse reponse = bvrClient.getBVRDemande(demande);
			// on essaie avec le débiteur 0 qui n'existe pas pour ne pas générer de nouveau numéro de BVR, la seule chose qui nous intéresse, c'est de recevoir une réponse
			Assert.isTrue(reponse.getMessage().contains("CONTRIB_ABSENT"));
			mav.addObject("bvrPlusStatus", "OK");
		}
		catch (Exception e) {
			String callstack = ExceptionUtils.extractCallStack(e);
			mav.addObject("bvrPlusStatus", "NOK\n\n" + HtmlHelper.renderMultilines(callstack));
		}
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
