package ch.vd.uniregctb.performance;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author <a href="mailto:sebastien.diaz@vd.ch">S�bastien Diaz </a> * @author <a href="mailto:sebastien.diaz@vd.ch">S�bastien Diaz</a>
 */
public class PerformanceLog implements Serializable {

//	private final static Logger LOGGER = Logger.getLogger(PerformanceLog.class);

	private static final long serialVersionUID = -7479127658002045950L;


	private final String name;

	private int hits;

	private long min;

	private long max;

	private long total;

	private long average;

	public PerformanceLog(String name) {
		this.name = name;
		this.min = Long.MAX_VALUE;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the hits.
	 */
	public long getHits() {
		return hits;
	}

	/**
	 * @return Returns the average.
	 */
	public long getAverage() {
		return average;
	}

	/**
	 * @return Returns the max.
	 */
	public long getMax() {
		return max;
	}

	/**
	 * @return Returns the min.
	 */
	public long getMin() {
		return min;
	}

	/**
	 * @return Returns the total.
	 */
	public long getTotal() {
		return total;
	}

	/**
	 * @param duration
	 */
	public void record(long duration) {
		if (duration < this.min) {
			this.min = duration;
		}
		if (duration > this.max) {
			this.max = duration;
		}

		this.hits++;
		this.total = total + duration;
		this.average = this.total / this.hits;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
}
