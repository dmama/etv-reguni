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

	/**
	 * Voir http://code.google.com/apis/chart/docs/chart_wizard.html
	 */
	@SuppressWarnings({"JavaDoc"})
	String buildGoogleChartUrl(String method) {

		final List<ResponseTimePeriode> time = results.get(method);

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
				.append(maxValues).append("&chdl=average|min|max").append("&chg=-1.3,-1,1,1").append("&chls=2|1,4,4|1,4,4").append("&chtt=").append(method)
				.append(" - Response Time Line (min/max/avg ms each hour)").toString();
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
