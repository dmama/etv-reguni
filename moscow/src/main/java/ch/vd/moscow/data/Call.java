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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

@Entity
@Table(name = "calls")
public class Call {

	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_FORMAT = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		}
	};

	private Long id;
	private Environment environment;
	private String service;
	private String caller;
	private String method;
	private long latency;
	private Date timestamp;
	private String params;

	public Call() {
	}

	public Call(Environment environment, String service, String caller, String method, long latency, Date timestamp, String params) {
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

	@Index(name = "ids_calls_service")
	@Column(name = "service", length = 20)
	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	@Index(name = "idx_calls_caller")
	@Column(name = "caller", length = 20)
	public String getCaller() {
		return caller;
	}

	public void setCaller(String caller) {
		this.caller = caller;
	}

	@Index(name = "ids_calls_method")
	@Column(name = "method", length = 50)
	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
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

	// exemple de ligne de log : [tiers2.read] INFO  [2010-11-11 10:48:38.464] [web-it] (15 ms) GetTiersHisto{login=UserLogin{userId='zsimsn', oid=22}, tiersNumber=10010169, parts=[ADRESSES]} charge=1
	public static Call parse(Environment environment, String line) throws java.text.ParseException {
		if (StringUtils.isBlank(line)) {
			return null;
		}

		int next;

		// on récupère le nom du service
		final String service;
		{
			int left = line.indexOf('[');
			int right = line.indexOf(']');
			service = line.substring(left + 1, right);
			next = right;
		}

		// on récupère le timestamp
		final String timestampAsString;
		{
			int left = line.indexOf('[', next + 1);
			int right = line.indexOf(']', next + 1);
			timestampAsString = line.substring(left + 1, right);
			next = right;
		}

		// on récupère le user
		String user;
		{
			int left = line.indexOf('[', next + 1);
			int right = line.indexOf(']', next + 1);
			user = line.substring(left + 1, right);
			next = right;
		}
		if (user.equals("aci-com")) {
			user = "acicom";
		}
		if (user.equals("emp-aci")) {
			user = "empaci";
		}

		// on récupère les millisecondes
		final String milliAsString;
		{
			int left = line.indexOf('(', next + 1);
			int right = line.indexOf(')', next + 1);
			milliAsString = line.substring(left + 1, right - 3);
			next = right;
		}

		// on récupère le nom de la méthode
		final String method;
		{
			int left = line.indexOf(' ', next + 1);
			int right = line.indexOf('{', next + 1);
			next = right;
			method = line.substring(left + 1, right);
		}

		// on récupère les paramètres
		final String params;
		{
			int left = next;
			int right = line.indexOf(" load=", next + 1);
			params = line.substring(left, right);
		}

		final Date timestamp = parseTimestamp(timestampAsString);
		final long milliseconds = Long.parseLong(milliAsString);

		return new Call(environment, service, user, method, milliseconds, timestamp, params);
	}

	public static Date parseTimestamp(String timestampAsString) throws ParseException {
		try {
			return TIMESTAMP_FORMAT.get().parse(timestampAsString);
		}
		catch (Exception e) {
			throw new RuntimeException("Error when parsing timestamp = [" + timestampAsString + "]", e);
		}
	}
}
