package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableLong;

/**
 * Analyse qui calcul et affiche l'évolution de la charge heure par heure (0-1h, 1-2h, 2-3h, ...).
 */
class LoadAnalyze extends Analyze {

	private Map<String, LoadData> results = new HashMap<String, LoadData>();

	public void addCall(Call call) {

		final String method = call.getMethod();

		LoadData data = results.get(method);
		if (data == null) {
			data = new LoadData();
			results.put(method, data);
		}

		data.addCall(call);
	}

	/**
	 * Voir http://code.google.com/apis/chart/docs/chart_wizard.html
	 */
	@SuppressWarnings({"JavaDoc"})
	Chart buildGoogleChart(String method) {

		final LoadData data = results.get(method);
		final List<LoadPoint> list = data.getList();
		if (list == null) {
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

		// On construit les différents lignes (total, empaci, sipf, ...) de valeurs
		final Map<String, ChartValues> valuesPerUser = new HashMap<String, ChartValues>();
		for (String user : data.getUsers()) {
			valuesPerUser.put(user, new ChartValues(list.size()));
		}
		final ChartValues totalValues = new ChartValues(list.size());

		for (int i = 0, timeSize = list.size(); i < timeSize; i++) {
			final LoadPoint point = list.get(i);
			// total
			totalValues.addValue(point.getTotalCount());
			// per user
			for (String user : data.getUsers()) {
				final MutableLong count = point.getCountPerUser().get(user);
				final ChartValues values = valuesPerUser.get(user);
				if (count == null) {
					values.addValue(0L);
				}
				else {
					values.addValue(count.longValue());
				}
			}
		}

		final Long max = totalValues.getMax();
		final String valuesRange = "0," + max;
		final String totalLabel = "total%20(" + totalValues.getTotal() + ")";

		final StringBuilder allValues = new StringBuilder();
		final StringBuilder allLabels = new StringBuilder();
		final StringBuilder allColors = new StringBuilder();
		final StringBuilder linesWidth = new StringBuilder();

		allValues.append("s:");
		allValues.append(totalValues.toSimpleEncoding(max));
		allLabels.append(totalLabel);
		allColors.append("000000");
		linesWidth.append("2");

		// Trie par ordre décroissant du nombre d'appels les données d'appels par méthode
		final List<Map.Entry<String, ChartValues>> entries = new ArrayList<Map.Entry<String, ChartValues>>(valuesPerUser.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, ChartValues>>() {
			public int compare(Map.Entry<String, ChartValues> o1, Map.Entry<String, ChartValues> o2) {
				return o2.getValue().getTotal().compareTo(o1.getValue().getTotal());
			}
		});

		for (Map.Entry<String, ChartValues> entry : entries) {
			allValues.append(',').append(entry.getValue().toSimpleEncoding(max));
			final String label = entry.getKey() + "%20(" + entry.getValue().getTotal() + ")";
			allLabels.append('|').append(label);
			allColors.append(',').append(Colors.forUser(entry.getKey()));
			linesWidth.append("|1");
		}

		final String url =
				new StringBuilder().append("http://chart.apis.google.com/chart?").append("chxl=1:").append(labels).append("&chxr=0,").append(valuesRange).append("&chxt=y,x&chxtc=1,4")
						.append("&chs=1000x200").append("&cht=lc").append("&chco=").append(allColors).append("&chds=").append(valuesRange).append("&chd=").append(allValues).append("&chdl=")
						.append(allLabels).append("&chg=-1.3,-1,1,1").append("&chls=").append(linesWidth).append("&chtt=").append(method).append("%20-%20Load%20(calls/quarter%20hour)")
						.toString();
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
			final LoadData data = results.get(method);
			for (LoadPoint t : data.getList()) {
				line.append(t).append(';');
			}
			System.out.println(line);
		}
	}

	@Override
	String name() {
		return "load";
	}
}