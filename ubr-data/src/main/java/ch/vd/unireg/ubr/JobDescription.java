package ch.vd.uniregctb.ubr;

import java.util.Date;
import java.util.List;

/**
 * Description d'un job tel qu'exposée par WS batch
 */
public class JobDescription {

	private String name;
	private String description;
	private JobStatus status;
	private Date lastStart;
	private Date lastEnd;
	private String runningMessage;
	private List<JobParamDescription> parameters;

	public JobDescription() {
	}

	public JobDescription(String name, String description, JobStatus status, Date lastStart, Date lastEnd, String runningMessage, List<JobParamDescription> parameters) {
		this.name = name;
		this.description = description;
		this.status = status;
		this.lastStart = lastStart;
		this.lastEnd = lastEnd;
		this.runningMessage = runningMessage;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public Date getLastStart() {
		return lastStart;
	}

	public void setLastStart(Date lastStart) {
		this.lastStart = lastStart;
	}

	public Date getLastEnd() {
		return lastEnd;
	}

	public void setLastEnd(Date lastEnd) {
		this.lastEnd = lastEnd;
	}

	public String getRunningMessage() {
		return runningMessage;
	}

	public void setRunningMessage(String runningMessage) {
		this.runningMessage = runningMessage;
	}

	public List<JobParamDescription> getParameters() {
		return parameters;
	}

	public void setParameters(List<JobParamDescription> parameters) {
		this.parameters = parameters;
	}
}
