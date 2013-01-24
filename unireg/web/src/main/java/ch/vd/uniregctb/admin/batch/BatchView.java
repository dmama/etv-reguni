package ch.vd.uniregctb.admin.batch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.scheduler.JobDefinition;

@SuppressWarnings("UnusedDeclaration")
public class BatchView {

	// static
	private String name;
	private String categorie;
	private String description;

	// dynamic
	private JobDefinition.JobStatut status;
	private Integer percentProgression;
	private String runningMessage;
	private Date lastStart;
	private Date lastEnd;
	private Date duration;
	private final Map<String, String> runningParams = new HashMap<String, String>();

	public BatchView(JobDefinition batch) {
		this.name = batch.getName();
		this.categorie = batch.getCategorie();
		this.description = batch.getDescription();

		this.status = batch.getStatut();
		this.percentProgression = batch.getPercentProgression();
		this.runningMessage = batch.getRunningMessage();
		this.lastStart = batch.getLastStart();
		this.lastEnd = batch.getLastEnd();

		final Map<String, Object> params = batch.getCurrentParametersDescription();
		if (params != null) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				this.runningParams.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
			}
		}
	}

	public String getName() {
		return name;
	}

	public String getCategorie() {
		return categorie;
	}

	public String getDescription() {
		return description;
	}

	public JobDefinition.JobStatut getStatus() {
		return status;
	}

	public Integer getPercentProgression() {
		return percentProgression;
	}

	public String getRunningMessage() {
		return runningMessage;
	}

	public Date getLastStart() {
		return lastStart;
	}

	public Date getLastEnd() {
		return lastEnd;
	}

	public Date getDuration() {
		return duration;
	}

	public Map<String, String> getRunningParams() {
		return runningParams;
	}
}
