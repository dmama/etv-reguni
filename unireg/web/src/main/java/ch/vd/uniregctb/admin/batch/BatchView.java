package ch.vd.uniregctb.admin.batch;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;

@SuppressWarnings("UnusedDeclaration")
public class BatchView {

	// static
	private String name;
	private JobCategory categorie;
	private String description;

	// dynamic
	private JobDefinition.JobStatut status;
	private Integer percentProgression;
	private String runningMessage;
	private Date lastStart;
	private Date lastEnd;
	private Long offset;
	private Date duration;
	private final Map<String, String> runningParams = new HashMap<>();

	public BatchView(JobDefinition batch) {
		this(batch, 0L);
	}

	public BatchView(JobDefinition batch, Long offsetArg) {
		this.name = batch.getName();
		this.categorie = batch.getCategorie();
		this.description = batch.getDescription();

		this.status = batch.getStatut();
		this.percentProgression = batch.getPercentProgression();
		this.runningMessage = batch.getRunningMessage();
		this.lastStart = batch.getLastStart();
		this.lastEnd = batch.getLastEnd();
		this.offset = offsetArg == null ? new Long(0L) : offsetArg;

		final Map<String, Object> params = batch.getCurrentParametersDescription();
		if (params != null) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				this.runningParams.put(entry.getKey(), valueToString(entry.getValue()));
			}
		}
	}

	@Nullable
	private static String valueToString(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String) {
			return (String) value;
		}
		else if (value instanceof Object[]) {
			final Object[] array = (Object[]) value;
			return String.join(", ", Arrays.stream(array)
					.map(Object::toString)
					.collect(Collectors.toList()));
		}
		else {
			return value.toString();
		}
	}

	public String getName() {
		return name;
	}

	public JobCategory getCategorie() {
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

	public Long getOffset() { return offset; }
}
