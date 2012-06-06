package ch.vd.moscow.controller.graph;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.data.category.DefaultCategoryDataset;

import ch.vd.moscow.database.CallStats;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PingDataSet {

	private List<CallDimension> breakouts = new ArrayList<CallDimension>();
	private final TimeResolution resolution;

	private Date minDate;
	private Date maxDate;
	private final Map<String, PingData> summaryData = new HashMap<String, PingData>();
	private Map<Cell, PingData> data = new HashMap<Cell, PingData>();

	private static class PingData {
		private int calls;
		private long accumulatedTime;
		private long maxPing;

		private PingData(int calls, long accumulatedTime, long maxPing) {
			this.calls = calls;
			this.accumulatedTime = accumulatedTime;
			this.maxPing = maxPing;
		}

		public void add(int calls, long accumulatedTime, long maxPing) {
			this.calls += calls;
			this.accumulatedTime += accumulatedTime;
			this.maxPing = Math.max(this.maxPing, maxPing);
		}

		public int getCalls() {
			return calls;
		}

		public long getAccumulatedTime() {
			return accumulatedTime;
		}

		public int getAveragePing() {
			return calls == 0 ? 0 : (int) (accumulatedTime / calls);
		}

		public int getMaxPing() {
			return (int) maxPing;
		}
	}

	private static class Cell {
		private String row;
		private Date column;

		private Cell(String row, Date column) {
			this.row = row;
			this.column = column;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final Cell cell = (Cell) o;

			return column.equals(cell.column) && row.equals(cell.row);

		}

		@Override
		public int hashCode() {
			int result = row.hashCode();
			result = 31 * result + column.hashCode();
			return result;
		}
	}

	private static class Label implements Comparable<Label> {
		private String criterionValue;
		private int calls;
		private int ping;
		private int maxPing;

		private Label(String criterionValue, int calls, int ping, int maxPing) {
			this.criterionValue = criterionValue;
			this.calls = calls;
			this.ping = ping;
			this.maxPing = maxPing;
		}

		@Override
		public int compareTo(Label o) {
			return Integer.valueOf(o.calls).compareTo(calls);
		}

		@Override
		public String toString() {
			return criterionValue + " (" + calls + " calls, avg " + ping + "ms, max " + maxPing + " ms)";
		}
	}


	/**
	 * Create an empty breakdown data set
	 *
	 * @param resolution the X-axis time resolution (in minutes)
	 */
	public PingDataSet(TimeResolution resolution) {
		this.resolution = resolution;
	}

	public void addBreakout(CallDimension criterion) {
		breakouts.add(criterion);
	}

	public void addCall(CallStats call) {

		final Date date = call.getDate();

		// update date boundaries
		minDate = (minDate == null || minDate.after(date)) ? date : minDate;
		maxDate = (maxDate == null || maxDate.before(date)) ? date : maxDate;

		// add value for the breakout decomposition if any
		if (!breakouts.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			final List<Object> coords = call.getCoord();
			for (int i = 0, criteriaSize = breakouts.size(); i < criteriaSize; i++) {
				if (first) {
					first = false;
				}
				else {
					sb.append("-");
				}
				sb.append(coords.get(i));
			}
			final String criterion = sb.toString();
			incSummary(criterion, call.getNbCalls(), call.getAccumulatedTime(), call.getMaxPing());

			final Cell cell = new Cell(criterion, date);
			addCalls(cell, call.getNbCalls(), call.getAccumulatedTime(), call.getMaxPing());
		}

		// add total value
		final Cell cell = new Cell("total", date);
		incSummary("total", call.getNbCalls(), call.getAccumulatedTime(), call.getMaxPing());
		addCalls(cell, call.getNbCalls(), call.getAccumulatedTime(), call.getMaxPing());
	}

	private void incSummary(String criterion, int calls, long accumulatedTime, long maxPing) {
		PingData total = summaryData.get(criterion);
		if (total == null) {
			summaryData.put(criterion, new PingData(calls, accumulatedTime, maxPing));
		}
		else {
			total.add(calls, accumulatedTime, maxPing);
		}
	}

	private void addCalls(Cell cell, int calls, long accumulatedTime, long maxPing) {
		final PingData value = data.get(cell);
		if (value == null) {
			data.put(cell, new PingData(calls, accumulatedTime, maxPing));
		}
		else {
			value.add(calls, accumulatedTime, maxPing);
		}
	}

	public void fill(DefaultCategoryDataset dataset) {

		if (data.isEmpty()) {
			return;
		}

		// sort labels by total calls decreasing order
		final List<Label> criterionLabels = new ArrayList<Label>();
		for (Map.Entry<String, PingData> entry : summaryData.entrySet()) {
			final PingData data = entry.getValue();
			criterionLabels.add(new Label(entry.getKey(), data.getCalls(), data.getAveragePing(), data.getMaxPing()));
		}
		Collections.sort(criterionLabels);

		final SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm");
		final SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

		// loop over each bucket of the timeline
		final Calendar cal = GregorianCalendar.getInstance();

		Date previousTime = null;
		for (cal.setTime(minDate); !cal.getTime().after(maxDate); cal.add(Calendar.MINUTE, resolution.getMinutes())) {
			final Date time = cal.getTime();

			// does not repeat the year-month-day part of label when looping within the same day
			final TimeKey timeKey;
			if (previousTime != null && isSameDay(previousTime, time)) {
				timeKey = new TimeKey(time, shortDateFormat.format(time));
			}
			else {
				timeKey = new TimeKey(time, fullDateFormat.format(time));
			}
			previousTime = time;

			// fill data for each criterion
			for (Label label : criterionLabels) {
				final PingData value = data.get(new Cell(label.criterionValue, time));
				dataset.addValue(value == null ? 0 : value.getAveragePing(), label.toString(), timeKey);
			}
		}
	}

	private static final ThreadLocal<GregorianCalendar> indexCal = new ThreadLocal<GregorianCalendar>() {
		@Override
		protected GregorianCalendar initialValue() {
			return (GregorianCalendar) GregorianCalendar.getInstance();
		}
	};

	private static boolean isSameDay(Date left, Date right) {
		return dateIndex(left) == dateIndex(right);
	}

	private static int dateIndex(Date date) {
		final GregorianCalendar cal = indexCal.get();
		cal.setTime(date);
		return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
	}
}
