package ch.vd.uniregctb.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;

import ch.vd.uniregctb.scheduler.BatchScheduler;

/**
 * Ce listener doit être placé après le listener du contexte Spring dans le fichier
 * web.xml de déploiement de la webapp.<p/>
 * Il sert à stopper proprement les batchs qui sont encore en cours au moment
 * de l'arrêt (propre) de la web-app
 */
public class JobMonitoringServletListener implements ServletContextListener {

	private final static Logger LOGGER = Logger.getLogger(JobMonitoringServletListener.class);

	private final static String beanName = "batchScheduler";

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		final Object objCtxt = sce.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (objCtxt instanceof WebApplicationContext) {
			final WebApplicationContext ctxt = (WebApplicationContext) objCtxt;
			final BatchScheduler scheduler = ctxt.getBean(beanName, BatchScheduler.class);
			scheduler.stopAllRunningJobs();
		}
		else {
			LOGGER.warn(String.format("Could not connect to application context (found %s)", objCtxt.getClass().getName()));
		}
	}
}
