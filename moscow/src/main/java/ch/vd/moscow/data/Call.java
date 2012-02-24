package ch.vd.moscow.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

@Entity
@Table(name = "calls")
public class Call {

	private Long id;
	private Environment environment;
	private Service service;
	private Caller caller;
	private Method method;
	private long latency;
	private Date timestamp;
	private String params;

	public Call() {
	}

	public Call(Environment environment, Service service, Caller caller, Method method, long latency, Date timestamp, String params) {
		this.environment = environment;
		this.service = service;
		this.caller = caller;
		this.method = method;
		this.latency = latency;
		this.timestamp = timestamp;
		this.params = params;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@ForeignKey(name = "fk_call_env_id")
	@JoinColumn(name = "env_id", nullable = false)
	@Index(name = "idx_calls_env", columnNames = "env_id")
	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@ManyToOne
	@ForeignKey(name = "fk_calls_service_id")
	@JoinColumn(name = "service_id", nullable = false)
	@Index(name = "idx_calls_service_id", columnNames = "service_id")
	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	@ManyToOne
	@ForeignKey(name = "fk_calls_caller_id")
	@JoinColumn(name = "caller_id", nullable = false)
	@Index(name = "idx_calls_caller_id", columnNames = "caller_id")
	public Caller getCaller() {
		return caller;
	}

	public void setCaller(Caller caller) {
		this.caller = caller;
	}

	@ManyToOne
	@ForeignKey(name = "fk_calls_method_id")
	@JoinColumn(name = "method_id", nullable = false)
	@Index(name = "idx_calls_method_id", columnNames = "method_id")
	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	@Column(name = "latency")
	public long getLatency() {
		return latency;
	}

	public void setLatency(long latency) {
		this.latency = latency;
	}

	@Index(name = "ids_calls_date")
	@Column(name = "date")
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Column(name = "params")
	@Lob
	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}
}
