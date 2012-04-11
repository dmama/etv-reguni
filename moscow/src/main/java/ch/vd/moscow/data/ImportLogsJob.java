package ch.vd.moscow.data;

import ch.vd.moscow.job.processor.ImportLogsProcessor;
import ch.vd.moscow.job.processor.JobProcessor;
import org.hibernate.annotations.Index;

import javax.persistence.*;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@DiscriminatorValue("ImportLogsJob")
public class ImportLogsJob extends JobDefinition {

	private LogDirectory directory;

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "logdir_id", nullable = false)
	@Index(name = "idx_job_logdir", columnNames = "logdir_id")
	public LogDirectory getDirectory() {
		return directory;
	}

	public void setDirectory(LogDirectory directory) {
		this.directory = directory;
	}

	@Transient
	@Override
	public Class<? extends JobProcessor> getProcessorClass() {
		return ImportLogsProcessor.class;
	}
}
