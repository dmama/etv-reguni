package ch.vd.uniregctb.ubr;

import java.util.List;

/**
 * Class exposée pour la liste des noms des jobs disponibles
 */
public class JobNames {

	private List<String> jobs;

	public JobNames() {
	}

	public JobNames(List<String> jobs) {
		this.jobs = jobs;
	}

	public List<String> getJobs() {
		return jobs;
	}

	public void setJobs(List<String> jobs) {
		this.jobs = jobs;
	}
}
