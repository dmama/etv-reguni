package ch.vd.moscow.controller.job;

import ch.vd.moscow.data.ImportLogsJob;
import ch.vd.moscow.data.JobDefinition;
import ch.vd.moscow.job.JobStatus;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JobView {

	private Long id;
	private String name;
	private String cronExpression;
	private String type;
	private JobStatusView status;
	private Long dirId;
	private String dirPath;

	public JobView() {
	}

	public JobView(JobDefinition job, JobStatus status) {
		this.id = job.getId();
		this.name = job.getName();
		this.cronExpression = job.getCronExpression();
		this.type = job.getClass().getSimpleName();
		this.status = status == null ? null : new JobStatusView(status);

		if (job instanceof ImportLogsJob) {
			final ImportLogsJob ij = (ImportLogsJob) job;
			this.dirId = ij.getDirectory().getId();
			this.dirPath = ij.getDirectory().getDirectoryPath();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public String getType() {
		return type;
	}

	public JobStatusView getStatus() {
		return status;
	}

	public Long getDirId() {
		return dirId;
	}

	public void setDirId(Long dirId) {
		this.dirId = dirId;
	}

	public String getDirPath() {
		return dirPath;
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}
}
