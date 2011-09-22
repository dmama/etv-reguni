package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PartsAnalyze extends Analyze {

	private static class MethodData {
		private final String name;
		private int callCount;
		private final Map<String, PartData> parts = new HashMap<String, PartData>();

		public MethodData(String name) {
			this.name = name;
		}

		public void addCall() {
			callCount++;
		}

		public void addPart(String part) {
			PartData pdata = parts.get(part);
			if (pdata == null) {
				pdata = new PartData(part);
				parts.put(part, pdata);
			}
			pdata.inc();
		}

		@SuppressWarnings({"UnusedDeclaration"})
		public String getName() {
			return name;
		}

		public int getCallCount() {
			return callCount;
		}

		public Map<String, PartData> getParts() {
			return parts;
		}
	}

	private static class PartData {
		private final String name;
		private int count;

		public PartData(String name) {
			this.name = name;
		}

		public void inc() {
			count++;
		}

		public String getName() {
			return name;
		}

		public int getCount() {
			return count;
		}
	}

	private final Map<String, MethodData> results = new HashMap<String, MethodData>();

	@Override
	void addCall(Call call) {

		final List<String> parts = call.getParts();
		if (parts != null) {
			MethodData mdata = results.get(call.getMethod());
			if (mdata == null) {
				mdata = new MethodData(call.getMethod());
				results.put(call.getMethod(), mdata);
			}

			mdata.addCall();
			for (String part : parts) {
				mdata.addPart(part);
			}
		}
	}

	@Override
	Chart buildGoogleChart(String method) {

		final MethodData data = results.get(method);
		if (data == null) {
			return null;
		}

		final long total = data.getCallCount();
		final List<PartData> list = new ArrayList<PartData>(data.getParts().values());

		//		final String labels = "|ADRESSES|ADRESSES_ENVOI|FORS_FISCAUX|FORS_FISCAUX_VIRTUELS|RAPPORTS_ENTRE_TIERS|SITUATIONS_FAMILLE";
		//		final String values = "50,30,10,60,65,190";
		StringBuilder labels = new StringBuilder();
		StringBuilder values = new StringBuilder();

		for (int i = 0, size = list.size(); i < size; i++) {
			final PartData pdata = list.get(i);
			final float perthousand = ((float) pdata.getCount()) * 1000.0f / (float) total;
			labels.append(String.format("%s%%20(%.1f%%%%20-%%20%d%%20calls)", pdata.getName(), perthousand / 10.0f, pdata.getCount()));
			values.append((int) perthousand / 10);
			if (i < size - 1) {
				labels.append("%7C"); // %7C = '|'
				values.append(',');
			}
		}

		final String title = method + "%20-%20Parts%20Usage%20Distribution";
		final String url =
				new StringBuilder().append("http://chart.apis.google.com/chart?chtt=").append(title).append("&chs=600x400&cht=p&chd=t:").append(values).append("&chdl=").append(labels).toString();
		return new Chart(url, 600, 400);
	}

	@Override
	void print() {
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
			MethodData mdata = results.get(method);
			for (PartData pdata : mdata.getParts().values()) {
				line.append(pdata.getName()).append("=").append(pdata.getCount()).append(';');
			}
			System.out.println(line);
		}
	}

	@Override
	String name() {
		return "parts";
	}
}
