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

class HistogramAnalyzer extends Analyzer {

	private Map<String, List<ResponseTimePeriode>> results = new HashMap<String, List<ResponseTimePeriode>>();

	private int lastPeriodeIndex = 0;

	public void addCall(String method, HourMinutes timestamp, long millisecondes) {

		if (millisecondes == 0) {
			// on ignore les appels qui prennent 0 millisecondes : il s'agit de valeurs non-représentatives retournées pas le cache.
			return;
		}

		List<ResponseTimePeriode> list = results.get(method);
		if (list == null) {
			list = new ArrayList<ResponseTimePeriode>();
			for (Periode periode : ResponseTimePeriode.DEFAULT_PERIODES) {
				list.add(new ResponseTimePeriode(periode));
			}
			results.put(method, list);
		}

		// optim : on commence à la position de la dernière période trouvée
		boolean found = false;
		for (int i = lastPeriodeIndex, listSize = list.size(); i < listSize; i++) {
			final ResponseTimePeriode periode = list.get(i);
			if (periode.isInPeriode(timestamp)) {
				periode.add(millisecondes);
				lastPeriodeIndex = i;
				found = true;
				break;
			}
		}
		if (!found) {
			// si on a pas trouvé, on recommence au début (ne devrait pas arriver, si les logs sont ordonnés de manière croissante dans le fichier)
			for (int i = 0; i < lastPeriodeIndex; i++) {
				final ResponseTimePeriode periode = list.get(i);
				if (periode.isInPeriode(timestamp)) {
					periode.add(millisecondes);
					lastPeriodeIndex = i;
					found = true;
					break;
				}
			}
		}
		Assert.isTrue(found);
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
				"    Les graphiques ci-dessous montrent les historiques des temps de réponse (min, max, moyenne) des appels (en millisecondes).<br/>\n";

		for (String method : methods) {
			final List<ResponseTimePeriode> time = results.get(method);
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
	private String buildGoogleChartUrl(String method, List<ResponseTimePeriode> time) {

		//		final String labels = "|00:00|01:00|02:00|03:00|04:00|05:00";
		StringBuilder labels = new StringBuilder();
		for (Periode periode : ResponseTimePeriode.DEFAULT_PERIODES) {
			labels.append("|").append(periode);
		}

		//		final String values = "50,30,10,60,65,190";
		StringBuilder minValues = new StringBuilder();
		StringBuilder maxValues = new StringBuilder();
		StringBuilder avgValues = new StringBuilder();
		long max = 0;
		for (int i = 0, timeSize = time.size(); i < timeSize; i++) {
			final ResponseTimePeriode range = time.get(i);
			minValues.append(range.getMin());
			if (i < timeSize - 1) {
				minValues.append(',');
			}
			maxValues.append(range.getMax());
			if (i < timeSize - 1) {
				maxValues.append(',');
			}
			avgValues.append(range.getAverage());
			if (i < timeSize - 1) {
				avgValues.append(',');
			}
			max = Math.max(max, range.getMax());
		}

		final String valuesRange = "0," + max;

		return new StringBuilder().append("http://chart.apis.google.com/chart?").append("chxl=1:").append(labels).append("&chxr=0,").append(valuesRange).append("&chxt=y,x").append("&chs=1000x200")
				.append("&cht=lc").append("&chco=000000,008000,AA0033").append("&chds=").append(valuesRange).append("&chd=t:").append(avgValues).append("|").append(minValues).append("|")
				.append(maxValues).append("&chdl=average|min|max").append("&chg=-1.3,-1,1,1").append("&chls=2|1,4,4|1,4,4").append("&chtt=").append(method).append("").toString();
	}

	public void print() {
		final List<String> methods = new ArrayList<String>(results.keySet());
		Collections.sort(methods);

		final StringBuilder header = new StringBuilder();
		header.append("Nom de méthode;");
		for (Periode periode : ResponseTimePeriode.DEFAULT_PERIODES) {
			header.append(periode).append(";");
		}
		System.out.println(header);

		for (String method : methods) {
			final StringBuilder line = new StringBuilder();
			line.append(method).append(";");
			final List<ResponseTimePeriode> time = results.get(method);
			for (ResponseTimePeriode t : time) {
				line.append(t).append(';');
			}
			System.out.println(line);
		}
	}
}
