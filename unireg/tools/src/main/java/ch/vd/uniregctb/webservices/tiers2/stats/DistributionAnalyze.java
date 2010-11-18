package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;

/**
 * Analyse qui calcul et affiche la distribution des temps de réponse par plage de temps de réponse (1-4 ms, 5-9 ms, 10-19 ms, ...).
 */
class DistributionAnalyze extends Analyze {

	private Map<String, List<ResponseTimeRange>> results = new HashMap<String, List<ResponseTimeRange>>();

	public void addCall(String method, HourMinutes timestamp, long milliseconds) {

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

	public void print() {

		final List<String> methods = new ArrayList<String>(results.keySet());
		Collections.sort(methods);

		final StringBuilder header = new StringBuilder();
		header.append("Nom de méthode;");
		for (TimeRange range : ResponseTimeRange.DEFAULT_TIME_RANGES) {
			header.append(range).append(";");
		}
		System.out.println(header);

		for (String method : methods) {
			final StringBuilder line = new StringBuilder();
			line.append(method).append(";");
			final List<ResponseTimeRange> time = results.get(method);
			for (ResponseTimeRange t : time) {
				line.append(t.getCount()).append(';');
			}
			System.out.println(line);
		}
	}

	/**
	 * Voir http://code.google.com/apis/chart/docs/chart_wizard.html
	 */
	@SuppressWarnings({"JavaDoc"})
	String buildGoogleChartUrl(String method) {

		final List<ResponseTimeRange> time = results.get(method);

		//		final String labels = "|0|1|2|3|4|5";
		StringBuilder labels = new StringBuilder();
		for (TimeRange range : ResponseTimeRange.DEFAULT_TIME_RANGES) {
			if (range.getEnd() == 0) {
				labels.append("| 0 ms");
			}
			else if (range.getEnd() == Long.MAX_VALUE) {
				labels.append("|>").append(range.getStart());
			}
			else {
				labels.append("|<").append(range.getEnd() + 1);
			}
		}

		//		final String values = "50,30,10,60,65,190";
		StringBuilder values = new StringBuilder();
		long max = 0;
		for (int i = 0, timeSize = time.size(); i < timeSize; i++) {
			final ResponseTimeRange range = time.get(i);
			values.append(range.getCount());
			if (i < timeSize - 1) {
				values.append(',');
			}
			max = Math.max(max, range.getCount());
		}

		//		final String valuesRange = "0,200";
		final String valuesRange = "0," + max;

		return new StringBuilder().append("http://chart.apis.google.com/chart?chxl=0:").append(labels).append("&chxr=1,").append(valuesRange)
				.append("&chxs=0,676767,8,0,l,676767&chxt=x,y&chbh=23,5&chs=1000x200&cht=bvg&chco=76A4FB&chds=").append(valuesRange).append("&chd=t:").append(values).append("&chg=20,50&chtt=")
				.append(method).append(" - Response Time Distribution (ms/call)").toString();
	}
}
