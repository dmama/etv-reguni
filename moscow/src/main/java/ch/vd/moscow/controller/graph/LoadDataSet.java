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

import org.apache.commons.lang.mutable.MutableInt;
import org.jfree.data.category.DefaultCategoryDataset;

import ch.vd.moscow.database.CallStats;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class LoadDataSet {

	private List<BreakdownCriterion> criteria = new ArrayList<BreakdownCriterion>();
	private final TimeResolution resolution;

	private Date minDate;
	private Date maxDate;
	private final Map<String, MutableInt> criterionTotalCalls = new HashMap<String, MutableInt>();

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
		private int totalCalls;

		private Label(String criterionValue, int totalCalls) {
			this.criterionValue = criterionValue;
			this.totalCalls = totalCalls;
		}

		@Override
		public int compareTo(Label o) {
			return Integer.valueOf(o.totalCalls).compareTo(totalCalls);
		}

		@Override
		public String toString() {
			return criterionValue + " (" + totalCalls + ')';
		}
	}

	private Map<Cell, MutableInt> data = new HashMap<Cell, MutableInt>();

	/**
	 * Create an empty breakdown data set
	 *
	 * @param resolution the X-axis time resolution (in minutes)
	 */
	public LoadDataSet(TimeResolution resolution) {
		this.resolution = resolution;
	}

	public void addBreakdown(BreakdownCriterion criterion) {
		criteria.add(criterion);
	}

	public void addCall(CallStats call) {

		final Date date = call.getDate();

		// update date boundaries
		minDate = (minDate == null || minDate.after(date)) ? date : minDate;
		maxDate = (maxDate == null || maxDate.before(date)) ? date : maxDate;

		// add value for the breakout decomposition if any
		if (!criteria.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			final List<Object> coords = call.getCoord();
			for (int i = 0, criteriaSize = criteria.size(); i < criteriaSize; i++) {
				if (first) {
					first = false;
				}
				else {
					sb.append("-");
				}
				sb.append(coords.get(i));
			}
			final String criterion = sb.toString();
			incTotal(criterion, call.getNbCalls());

			final Cell cell = new Cell(criterion, date);
			addCalls(cell, call.getNbCalls());
		}

		// add total value
		final Cell cell = new Cell("total", date);
		incTotal("total", call.getNbCalls());
		addCalls(cell, call.getNbCalls());
	}

	private void incTotal(String criterion, int increment) {
		MutableInt total = criterionTotalCalls.get(criterion);
		if (total == null) {
			criterionTotalCalls.put(criterion, new MutableInt(increment));
		}
		else {
			total.add(increment);
		}
	}

	private void addCalls(Cell cell, int count) {
		final MutableInt value = data.get(cell);
		if (value == null) {
			data.put(cell, new MutableInt(count));
		}
		else {
			value.add(count);
		}
	}

	public void fill(DefaultCategoryDataset dataset) {

		if (data.isEmpty()) {
			return;
		}

		// sort labels by total calls decreasing order
		final List<Label> criterionLabels = new ArrayList<Label>();
		for (Map.Entry<String, MutableInt> entry : criterionTotalCalls.entrySet()) {
			criterionLabels.add(new Label(entry.getKey(), entry.getValue().intValue()));
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
				final MutableInt value = data.get(new Cell(label.criterionValue, time));
				dataset.addValue(value == null ? 0 : value.intValue(), label.toString(), timeKey);
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
