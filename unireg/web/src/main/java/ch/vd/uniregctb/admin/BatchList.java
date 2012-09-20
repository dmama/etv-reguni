package ch.vd.uniregctb.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.support.MessageSourceAccessor;

import ch.vd.uniregctb.scheduler.JobDefinition;

/**
 * Form backing object de la page de démarrage des batches
 */
public class BatchList {

	private final List<GestionJob> jobs;

	private Map<String, Object> startParams = new HashMap<String, Object>();

	public  BatchList(List<JobDefinition> jobs, MessageSourceAccessor messageSource) {
		this.jobs = new ArrayList<GestionJob>();
		for (JobDefinition d : jobs) {

			GestionJob job = new GestionJob(d, messageSource);
			this.jobs.add(job);
		}

	}

	/**
	 * @return the jobs
	 */
	public List<GestionJob> getJobs() {
		return jobs;
	}

	/**
	 * @return les paramètres de démarrage du batch sélectionné
	 */
	public Map<String, Object> getStartParams() {
		return startParams;
	}

	public void setStartParams(Map<String, Object> startParams) {
		this.startParams = startParams;
	}
}
