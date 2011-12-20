package ch.vd.moscow.data;

import ch.vd.moscow.job.processor.JobProcessor;

import javax.persistence.*;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@Table(name = "jobs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn
public abstract class JobDefinition {

	public static final String KEY_JOB = "JobDefinition";
	public static final String KEY_MANAGER = "JobManager";

	private Long id;
	private String name;
	private String cronExpression;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "name", length = 30)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "cron", length = 15)
	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	/**
	 * @return class of processor to use to run the job
	 */
	@Transient
	public abstract Class<? extends JobProcessor> getProcessorClass();
}
