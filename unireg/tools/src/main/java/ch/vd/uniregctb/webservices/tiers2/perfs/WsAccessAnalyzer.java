package ch.vd.uniregctb.webservices.tiers2.perfs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.utils.Assert;

public class WsAccessAnalyzer {

	private static class TimeRange implements Comparable<TimeRange> {

		private final long start;
		private final long end;

		private TimeRange(long start, long end) {
			this.start = start;
			this.end = end;
		}

		public long getStart() {
			return start;
		}

		public long getEnd() {
			return end;
		}

		public boolean isInRange(long milliseconds) {
			return start <= milliseconds && milliseconds <= end;
		}

		public int compareTo(TimeRange o) {
			return (start < o.start ? -1 : (start == o.start ? 0 : 1));
		}

		@Override
		public String toString() {
			if (end == Long.MAX_VALUE) {
				return String.format(">= %d ms", start);
			}
			else {
				return String.format("%d-%d ms", start, end);
			}
		}
	}

	private static class ResponseTimeRange implements Comparable<ResponseTimeRange> {

		private static final TimeRange DEFAULT_TIME_RANGES[] =
				{new TimeRange(0, 0), new TimeRange(1, 4), new TimeRange(5, 9), new TimeRange(10, 19), new TimeRange(20, 29), new TimeRange(30, 39), new TimeRange(40, 49), new TimeRange(50, 74),
						new TimeRange(75, 99), new TimeRange(100, 124), new TimeRange(125, 149), new TimeRange(150, 199), new TimeRange(200, 249), new TimeRange(250, 299), new TimeRange(300, 349),
						new TimeRange(350, 399), new TimeRange(400, 459), new TimeRange(450, 499), new TimeRange(500, 749), new TimeRange(720, 999), new TimeRange(1000, 1499),
						new TimeRange(1500, 1999), new TimeRange(2000, 4999), new TimeRange(5000, 9999), new TimeRange(10000, 49999), new TimeRange(50000, Long.MAX_VALUE)};

		private final TimeRange range;
		private long count;

		private ResponseTimeRange(TimeRange range) {
			Assert.notNull(range);
			this.range = range;
		}

		public void incCount() {
			count++;
		}

		public long getCount() {
			return count;
		}

		public TimeRange getRange() {
			return range;
		}

		public boolean isInRange(long milliseconds) {
			return range.isInRange(milliseconds);
		}

		public int compareTo(ResponseTimeRange o) {
			return this.range.compareTo(o.range);
		}

		@Override
		public String toString() {
			return String.format("%s, %d", range, count);
		}
	}

	private Map<String, List<ResponseTimeRange>> results = new HashMap<String, List<ResponseTimeRange>>();

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage:\n./ws-analyzer.sh ws-access.log0 ws-access.log1 ws-access.log2 ...");
			return;
		}

		WsAccessAnalyzer analyzer = new WsAccessAnalyzer();
		analyzer.analyze(args);
		analyzer.print();

	}

	private void print() {

		for (Map.Entry<String, List<ResponseTimeRange>> entry : results.entrySet()) {

			System.out.println("=> " + entry.getKey() + " (nombre d'appels) :");

			final List<ResponseTimeRange> list = entry.getValue();
			Collections.sort(list);

			for (ResponseTimeRange r : list) {
				System.out.println(r);
			}

			System.out.println();
		}
	}

	private void analyze(String[] args) {

		try {
			for (String filename : args) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
				String line = reader.readLine();
				while (line != null) {
					process(line);
					line = reader.readLine();
				}

				reader.close();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	//	private static final Pattern PATTERN = Pattern.compile(".*\\(([0-9]*) ms\\) ([^\\{]*).*");
	private static final Pattern PATTERN = Pattern.compile(".*\\(([0-9]+) ms\\) ([^\\{]*).*");

	private void process(String line) {

		try {
			final Matcher matcher = PATTERN.matcher(line);
			if (matcher.matches()) {
				final long milliseconds = Long.parseLong(matcher.group(1));
				final String method = matcher.group(2);

				addCall(method, milliseconds);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addCall(String method, long milliseconds) {

		List<ResponseTimeRange> list = results.get(method);
		if (list == null) {
			list = new ArrayList<ResponseTimeRange>();
			for (TimeRange timeRange : ResponseTimeRange.DEFAULT_TIME_RANGES) {
				list.add(new ResponseTimeRange(timeRange));
			}
			results.put(method, list);
		}

		boolean found = false;
		for (ResponseTimeRange range : list) {
			if (range.isInRange(milliseconds)) {
				range.incCount();
				found = true;
				break;
			}
		}
		Assert.isTrue(found);
	}
}
