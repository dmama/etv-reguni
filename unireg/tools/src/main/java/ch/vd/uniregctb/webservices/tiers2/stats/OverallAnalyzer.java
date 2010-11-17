package ch.vd.uniregctb.webservices.tiers2.stats;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;

class OverallAnalyzer extends Analyzer {

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

	public void printHtml(String htmlFile, boolean localImages) throws IOException {

		if (!htmlFile.toLowerCase().endsWith(".html") && !htmlFile.toLowerCase().endsWith(".htm")) {
			htmlFile += ".html";
		}

		final List<String> methods = new ArrayList<String>(results.keySet());
		Collections.sort(methods);

		String content = "<html>\n" +
				"  <head>\n" +
				"    <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">\n" +
				"    <title>Temps de réponse du web-service Tiers 2</title>\n" +
				"    <script language=\"javascript\" src=\"http://www.google.com/jsapi\"></script>\n" +
				"  </head>\n" +
				"  <body>\n" +
				"    <h1>Temps de réponse du web-service Tiers 2</h1>\n" +
				"    Les graphiques ci-dessous montrent la décomposition des appels en fonction du temps de réponse (en millisecondes).<br/>\n";

		for (String method : methods) {
			final List<ResponseTimeRange> time = results.get(method);
			content += "    " + buildChart(method, htmlFile, localImages, buildGoogleChartUrl(method, time)) + "\n";
		}

		content += "    <br/>\n" +
				"    (Dernière mise-à-jour le " + new SimpleDateFormat("dd.MM.yyyy à HH:mm:ss").format(new Date()) + ")\n" +
				"  </body>\n" +
				"</html>";

		final FileWriter writer = new FileWriter(htmlFile);
		writer.write(content);
		writer.close();
	}

	/**
	 * Voir http://code.google.com/apis/chart/docs/chart_wizard.html
	 */
	@SuppressWarnings({"JavaDoc"})
	private String buildGoogleChartUrl(String method, List<ResponseTimeRange> time) {

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
				.append(method).toString();
	}
}
