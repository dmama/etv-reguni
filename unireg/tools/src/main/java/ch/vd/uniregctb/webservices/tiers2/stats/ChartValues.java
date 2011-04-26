package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Liste de valeurs devant être représentées dans un graphique Google (http://imagecharteditor.appspot.com/).
 * <p>
 * Cette classe permet de générer directement la string qui encode de manière simple ou étendue les valeurs.
 */
public class ChartValues {

	private final List<Long> values;
	private Long max;
	private Long total;

	public ChartValues(List<Long> values) {
		this.values = values;
		this.max = 0L;
		this.total = 0L;
		for (Long value : values) {
			this.max = Math.max(this.max, value);
			this.total += value;
		}
	}

	public ChartValues(int count) {
		this.values = new ArrayList<Long>(count);
		this.max = 0L;
		this.total = 0L;
	}

	public void addValue(Long val) {
		values.add(val);
		this.max = Math.max(this.max, val);
		this.total += val;
	}

	public String toExtendedEncoding(long max) {
		return extendedEncode(values, max);
	}

	public String toSimpleEncoding(long max) {
		return simpleEncode(values, max);
	}

	public Long getMax() {
		return max;
	}

	public Long getTotal() {
		return total;
	}

	private static String simpleEncoding = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	/**
	 * from http://code.google.com/apis/chart/docs/data_formats.html#encoding_data
	 */
	private static String simpleEncode(List<Long> values, Long maxValue) {
		StringBuilder chartData = new StringBuilder();

		for (Long currentValue : values) {
			if (currentValue >= 0L) {
				final long index = Math.round(((double) (simpleEncoding.length() - 1)) * currentValue.doubleValue() / maxValue.doubleValue());
				chartData.append(simpleEncoding.charAt((int) index));
			}
			else {
				chartData.append('_');
			}
		}
		return chartData.toString();
	}
	// Same as simple encoding, but for extended encoding.
	private static String EXTENDED_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.";
	private static double EXTENDED_MAP_LENGTH = EXTENDED_MAP.length();

	/**
	 * from http://code.google.com/apis/chart/docs/data_formats.html#encoding_data
	 */
	private static String extendedEncode(List<Long> values, Long maxValue) {
		StringBuilder chartData = new StringBuilder();

		for (int i = 0, len = values.size(); i < len; i++) {
			Long numericVal = values.get(i);
			// Scale the value to maxVal.
			Double scaledVal = Math.floor(EXTENDED_MAP_LENGTH * EXTENDED_MAP_LENGTH * numericVal.doubleValue() / maxValue.doubleValue());

			if (scaledVal > (EXTENDED_MAP_LENGTH * EXTENDED_MAP_LENGTH) - 1) {
				chartData.append("..");
			}
			else if (scaledVal < 0.0f) {
				chartData.append("__");
			}
			else {
				// Calculate first and second digits and add them to the output.
				double quotient = Math.floor(scaledVal / EXTENDED_MAP_LENGTH);
				double remainder = scaledVal - EXTENDED_MAP_LENGTH * quotient;
				chartData.append(EXTENDED_MAP.charAt((int) quotient));
				chartData.append(EXTENDED_MAP.charAt((int) remainder));
			}
		}

		return chartData.toString();
	}
}
