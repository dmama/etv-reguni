package ch.vd.uniregctb.scheduler;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean qui permet d'enregistrer des jobs quartz en utilisant la syntaxe des expressions cron.
 */
public class CronTab implements InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(CronTab.class);

	public static class CronDefinition {
		private JobDefinition job;
		private Map<String, Object> params;
		private String cronExpression;

		public JobDefinition getJob() {
			return job;
		}

		@SuppressWarnings({"UnusedDeclaration"})
		public void setJob(JobDefinition job) {
			this.job = job;
		}

		public Map<String, Object> getParams() {
			return params;
		}

		@SuppressWarnings({"UnusedDeclaration"})
		public void setParams(Map<String, Object> params) {
			this.params = params;
		}

		public String getCronExpression() {
			return cronExpression;
		}

		@SuppressWarnings({"UnusedDeclaration"})
		public void setCronExpression(String cronExpression) {
			this.cronExpression = cronExpression;
		}
	}

	private List<CronDefinition> definitions;
	private BatchScheduler scheduler;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDefinitions(List<CronDefinition> definitions) {
		this.definitions = definitions;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setScheduler(BatchScheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
		if (definitions == null || definitions.isEmpty()) {
			return;
		}

		for (CronDefinition def : definitions) {
			final JobDefinition job = def.getJob();
			final String expression = def.getCronExpression();
			if (StringUtils.isBlank(expression)) {
				LOGGER.warn("L'expression cron du job " + job.getName() + " est vide : le cron est ignoré.");
			}
			else {
				LOGGER.info("Enregistrement du cron [" + expression + "] sur le job " + job.getName());
				scheduler.registerCron(job, def.getParams(), expression);
			}
		}
	}
}
