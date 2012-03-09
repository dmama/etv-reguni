package ch.vd.moscow.data;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

import org.hibernate.annotations.Index;

@Entity
@Table(name = "completion_statuses")
public class CompletionStatus {

	private Long id;
	private Environment environment;
	private Date upTo;

	public CompletionStatus() {
	}

	public CompletionStatus(Environment environment, Date upTo) {
		this.environment = environment;
		this.upTo = upTo;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "env_id", nullable = false)
	@Index(name = "idx_cstatus_env", columnNames = "env_id")
	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Column(name = "up_to")
	public Date getUpTo() {
		return upTo;
	}

	public void setUpTo(Date upTo) {
		this.upTo = upTo;
	}
}
