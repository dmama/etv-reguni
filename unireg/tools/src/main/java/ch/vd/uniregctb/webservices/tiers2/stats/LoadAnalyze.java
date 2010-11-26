package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;

/**
 * Analyse qui calcul et affiche l'évolution de la charge heure par heure (0-1h, 1-2h, 2-3h, ...).
 */
class LoadAnalyze extends Analyze {

	private Map<String, List<LoadData>> results = new HashMap<String, List<LoadData>>();

	private int lastPeriodeIndex = 0;

	public void addCall(Call call) {

		final String method = call.getMethod();
		final HourMinutes timestamp = call.getTimestamp();

		List<LoadData> list = results.get(method);
		if (list == null) {
			list = new ArrayList<LoadData>();
			for (Periode periode : Periode.DEFAULT_PERIODES) {
				list.add(new LoadData(periode));
			}
			results.put(method, list);
		}

		// optim : on commence à la position de la dernière période trouvée
		boolean found = false;
		for (int i = lastPeriodeIndex, listSize = list.size(); i < listSize; i++) {
			final LoadData data = list.get(i);
			if (data.isInPeriode(timestamp)) {
				data.add();
				lastPeriodeIndex = i;
				found = true;
				break;
			}
		}
		if (!found) {
			// si on a pas trouvé, on recommence au début (ne devrait pas arriver, si les logs sont ordonnés de manière croissante dans le fichier)
			for (int i = 0; i < lastPeriodeIndex; i++) {
				final LoadData data = list.get(i);
				if (data.isInPeriode(timestamp)) {
					data.add();
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

		final List<LoadData> data = results.get(method);
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
		StringBuilder values = new StringBuilder();
		long max = 0;
		for (int i = 0, timeSize = data.size(); i < timeSize; i++) {
			final LoadData range = data.get(i);
			values.append(range.getCount());
			if (i < timeSize - 1) {
				values.append(',');
			}
			max = Math.max(max, range.getCount());
		}

		final String valuesRange = "0," + max;

		final String url =
				new StringBuilder().append("http://chart.apis.google.com/chart?").append("chxl=1:").append(labels).append("&chxr=0,").append(valuesRange).append("&chxt=y,x&chxtc=1,4")
						.append("&chs=1000x200").append("&cht=lc").append("&chco=3D7930").append("&chds=").append(valuesRange).append("&chd=t:").append(values).append("&chdl=calls")
						.append("&chg=-1.3,-1,1,1").append("&chls=1").append("&chtt=").append(method).append("%20-%20Load%20(calls/quarter%20hour)").toString();
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
			final List<LoadData> data = results.get(method);
			for (LoadData t : data) {
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