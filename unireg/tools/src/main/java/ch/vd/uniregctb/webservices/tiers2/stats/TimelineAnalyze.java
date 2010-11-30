package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;

/**
 * Analyse qui calcul et affiche l'évolution des temps de réponse heure par heure (0-1h, 1-2h, 2-3h, ...).
 */
class TimelineAnalyze extends Analyze {

	private Map<String, List<TimelineData>> results = new HashMap<String, List<TimelineData>>();

	private int lastPeriodeIndex = 0;

	public void addCall(Call call) {

		final long milliseconds = call.getMilliseconds() / call.getTiersCount();
		if (milliseconds == 0) {
			// on ignore les appels qui prennent 0 millisecondes : il s'agit de valeurs non-représentatives retournées pas le cache.
			return;
		}

		final String method = call.getMethod();
		final HourMinutes timestamp = call.getTimestamp();

		List<TimelineData> list = results.get(method);
		if (list == null) {
			list = new ArrayList<TimelineData>();
			for (Periode periode : Periode.DEFAULT_PERIODES) {
				list.add(new TimelineData(periode));
			}
			results.put(method, list);
		}

		// optim : on commence à la position de la dernière période trouvée
		boolean found = false;
		for (int i = lastPeriodeIndex, listSize = list.size(); i < listSize; i++) {
			final TimelineData data = list.get(i);
			if (data.isInPeriode(timestamp)) {
				data.add(milliseconds);
				lastPeriodeIndex = i;
				found = true;
				break;
			}
		}
		if (!found) {
			// si on a pas trouvé, on recommence au début (ne devrait pas arriver, si les logs sont ordonnés de manière croissante dans le fichier)
			for (int i = 0; i < lastPeriodeIndex; i++) {
				final TimelineData data = list.get(i);
				if (data.isInPeriode(timestamp)) {
					data.add(milliseconds);
					lastPeriodeIndex = i;
					found = true;
					break;
				}
			}
		}
		Assert.isTrue(found);
	}

	/**
	 * Voir http://code.google.com/apis/chart/docs/chart_wizard.html
	 */
	@SuppressWarnings({"JavaDoc"})
	Chart buildGoogleChart(String method) {

		final List<TimelineData> data = results.get(method);
		if (data == null) {
			return null;
		}

		//		final String labels = "|00:00|01:00|02:00|03:00|04:00|05:00";
		StringBuilder labels = new StringBuilder();
		for (int i = 0; i < Periode.DEFAULT_PERIODES.length; i++) {
			labels.append("|");
			if (i % 4 == 0) { // on ne met un label que sur les heures piles
				final Periode periode = Periode.DEFAULT_PERIODES[i];
				labels.append(periode);
			}
		}

		//		final String values = "50,30,10,60,65,190";
		StringBuilder minValues = new StringBuilder();
		StringBuilder maxValues = new StringBuilder();
		StringBuilder avgValues = new StringBuilder();
		long min = Long.MAX_VALUE;
		long max = 0;
		long total = 0;
		long count = 0;
		for (int i = 0, timeSize = data.size(); i < timeSize; i++) {
			final TimelineData range = data.get(i);
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
			if (range.getMin() > 0) {
				min = Math.min(min, range.getMin());
			}
			total += range.getTotal();
			count += range.getCount();
		}

		final String valuesRange = "0," + max;

		final String averageLabel = "average%20(" + (total / count) + "%20ms)";
		final String minLabel = "min%20(" + min + "%20ms)";
		final String maxLabel = "max%20(" + max + "%20ms)";
		final String url =
				new StringBuilder().append("http://chart.apis.google.com/chart?").append("chxl=1:").append(labels).append("&chxr=0,").append(valuesRange).append("&chxt=y,x&chxtc=1,4")
						.append("&chs=1000x200").append("&cht=lc").append("&chco=000000,008000,AA0033").append("&chds=").append(valuesRange).append("&chd=t:").append(avgValues).append("|")
						.append(minValues).append("|").append(maxValues).append("&chdl=").append(averageLabel).append("|").append(minLabel).append("|").append(maxLabel).append("&chg=-1.3,-1,1,1")
						.append("&chls=2|1,4,4|1,4,4").append("&chtt=").append(method).append("%20-%20Response%20Time%20Line%20(min/max/avg%20ms/quarter%20hour)").toString();
		return new Chart(url, 1000, 200);
	}

	public void print() {
		final List<String> methods = new ArrayList<String>(results.keySet());
		Collections.sort(methods);

		final StringBuilder header = new StringBuilder();
		header.append("Nom de méthode;");
		for (Periode periode : Periode.DEFAULT_PERIODES) {
			header.append(periode).append(";");
		}
		System.out.println(header);

		for (String method : methods) {
			final StringBuilder line = new StringBuilder();
			line.append(method).append(";");
			final List<TimelineData> data = results.get(method);
			for (TimelineData t : data) {
				line.append(t).append(';');
			}
			System.out.println(line);
		}
	}

	@Override
	String name() {
		return "timeline";
	}
}
