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

	private final Map<String, List<DistributionData>> results = new HashMap<String, List<DistributionData>>();

	@Override
	public void addCall(Call call) {

		final String method = call.getMethod();
		final long milliseconds = call.getMilliseconds() / call.getTiersCount();

		List<DistributionData> list = results.get(call.getMethod());
		if (list == null) {
			list = new ArrayList<DistributionData>();
			for (TimeRange timeRange : DistributionData.DEFAULT_TIME_RANGES) {
				list.add(new DistributionData(timeRange));
			}
			results.put(method, list);
		}

		boolean found = false;
		for (DistributionData range : list) {
			if (range.isInRange(milliseconds)) {
				range.incCount();
				found = true;
				break;
			}
		}
		Assert.isTrue(found);
	}

	@Override
	public void print() {

		final List<String> methods = new ArrayList<String>(results.keySet());
		Collections.sort(methods);

		final StringBuilder header = new StringBuilder();
		header.append("Nom de méthode;");
		for (TimeRange range : DistributionData.DEFAULT_TIME_RANGES) {
			header.append(range).append(";");
		}
		System.out.println(header);

		for (String method : methods) {
			final StringBuilder line = new StringBuilder();
			line.append(method).append(";");
			final List<DistributionData> time = results.get(method);
			for (DistributionData data : time) {
				line.append(data.getCount()).append(';');
			}
			System.out.println(line);
		}
	}

	/**
	 * Voir http://code.google.com/apis/chart/docs/chart_wizard.html
	 */
	@Override
	@SuppressWarnings({"JavaDoc"})
	Chart buildGoogleChart(String method) {

		final List<DistributionData> data = results.get(method);
		if (data == null) {
			return null;
		}

		//		final String labels = "|0|1|2|3|4|5";
		StringBuilder labels = new StringBuilder();
		for (TimeRange range : DistributionData.DEFAULT_TIME_RANGES) {
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
		for (int i = 0, timeSize = data.size(); i < timeSize; i++) {
			final DistributionData range = data.get(i);
			values.append(range.getCount());
			if (i < timeSize - 1) {
				values.append(',');
			}
			max = Math.max(max, range.getCount());
		}

		//		final String valuesRange = "0,200";
		final String valuesRange = "0," + max;

		final String url = new StringBuilder().append("http://chart.apis.google.com/chart?chxl=0:").append(labels).append("&chxr=1,").append(valuesRange)
				.append("&chxs=0,676767,8,0,l,676767&chxt=x,y&chbh=23,5&chs=1000x200&cht=bvg&chco=76A4FB&chds=").append(valuesRange).append("&chd=t:").append(values).append("&chg=20,50&chtt=")
				.append(method).append("%20-%20Response%20Time%20Distribution%20(ms/call)").toString();
		return new Chart(url, 1000, 200);
	}

	@Override
	String name() {
		return "distribution";
	}
}
