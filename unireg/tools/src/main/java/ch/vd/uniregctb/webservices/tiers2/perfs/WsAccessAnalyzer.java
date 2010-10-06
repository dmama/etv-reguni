package ch.vd.uniregctb.webservices.tiers2.perfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.FileCopyUtils;

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
						new TimeRange(1500, 1999), new TimeRange(2000, 4999), new TimeRange(5000, 9999), new TimeRange(10000, 29999), new TimeRange(30000, 59999),
						new TimeRange(60000, Long.MAX_VALUE)};

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

	public static void main(String[] args) throws Exception {

		final CommandLine commandLine = parseCommandLine(args);
		if (commandLine == null) {
			return;
		}

		final String htmlFile = commandLine.getOptionValue("html");
		final boolean localImages = commandLine.hasOption("localImages");
		final String[] files = commandLine.getArgs();
		final String proxy = commandLine.getOptionValue("proxy");

		if (StringUtils.isNotBlank(proxy)) {
			final String[] p = proxy.split(":");
			System.setProperty("http.proxyHost", p[0]);
			System.setProperty("http.proxyPort", p[1]);
		}

		final WsAccessAnalyzer analyzer = new WsAccessAnalyzer();
		analyzer.analyze(files);

		if (StringUtils.isNotBlank(htmlFile)) {
			analyzer.printHtml(htmlFile, localImages);
		}
		else {
			analyzer.print();
		}

	}

	@SuppressWarnings({"static-access", "AccessStaticViaInstance"})
	private static CommandLine parseCommandLine(String[] args) {

		final CommandLine line;
		try {
			CommandLineParser parser = new GnuParser();
			Option help = new Option("help", "display this help");
			Option html = OptionBuilder.withArgName("file").hasArg().withDescription("output results in a html file").create("html");
			Option localImages = new Option("localImages", "store images in local folder (with -html)");
			Option proxy = OptionBuilder.withArgName("host:port").hasArg().withDescription("use HTTP proxy on given port").create("proxy");

			Options options = new Options();
			options.addOption(help);
			options.addOption(html);
			options.addOption(localImages);
			options.addOption(proxy);

			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || line.getArgs().length == 0) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("WsAccessAnalyzer [options] FILE...", "Options:", options, null);
				return null;
			}
		}
		catch (ParseException exp) {
			System.err.println("Erreur de parsing.  Raison: " + exp.getMessage());
			return null;
		}

		return line;

	}

	private void print() {

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

	private void printHtml(String htmlFile, boolean localImages) throws IOException {

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
			content += "    " + buildChart(method, time, htmlFile, localImages) + "\n";
		}

		content += "    <br/>\n" +
				"    (Dernière mise-à-jour le " + new SimpleDateFormat("dd.MM.yyyy à HH:mm:ss").format(new Date()) + ")\n" +
				"  </body>\n" +
				"</html>";

		final FileWriter writer = new FileWriter(htmlFile);
		writer.write(content);
		writer.close();
	}

	private String buildChart(String method, List<ResponseTimeRange> time, String htmlFile, boolean localImages) throws IOException {

		final String url = buildGoogleChartUrl(method, time);
		
		if (localImages) {
			final String dirname = FilenameUtils.removeExtension(htmlFile);

			// on crée un sous-répertoire du même nom (sans l'extension) du fichier html
			final File dir = new File(dirname);
			if (!dir.exists() || !dir.isDirectory()) {
				if (!dir.mkdirs()) {
					throw new RuntimeException("Unable to create directoy [" + dirname + "]");
				}
			}

			// on récupère l'image générée par Google et on la stocke dans le sous-répertoire
			final String imagename = dirname + "/" + method + ".png";
			InputStream is = null;
			OutputStream os = null;
			try {
				URL u = new URL(url);
				is = u.openStream();
				os = new FileOutputStream(imagename, false);
				FileCopyUtils.copy(is, os);
			}
			finally {
				if (is != null) {
					is.close();
				}
				if (os != null) {
					os.close();
				}
			}

			// on inclut l'image stockée en local
			final String imageurl = FilenameUtils.getName(dirname) + "/" + method + ".png";
			return "<img src=\"" + imageurl + "\" width=\"1000\" height=\"200\" alt=\"" + method + "\"/><br/><br/><br/>";
		}
		else {
			return "<img src=\"" + url + "\" width=\"1000\" height=\"200\" alt=\"" + method + "\"/><br/><br/><br/>";
		}
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

	private static final Pattern MILLI_METHOD = Pattern.compile(".*\\(([0-9]+) ms\\) ([^\\{]*).*");
	private static final Pattern TIERS_NUMBERS = Pattern.compile(".*tiersNumbers=\\[([0-9, ]*)\\].*");

	private void process(String line) {

		try {
			final Matcher matcher = MILLI_METHOD.matcher(line);
			if (matcher.matches()) {
				final long milliseconds = Long.parseLong(matcher.group(1));
				final String method = matcher.group(2);

				final int tiersCount;
				if (method.startsWith("GetBatch")) {
					// en cas de méthode batch, on calcul le temps moyen de réponse par tiers demandé
					final Matcher m = TIERS_NUMBERS.matcher(line);
					Assert.isTrue(m.matches());
					final String tiersNumer = m.group(1);
					tiersCount = StringUtils.countMatches(tiersNumer, ",") + 1;
				}
				else {
					tiersCount = 1;
				}

				addCall(method, milliseconds / tiersCount);
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
