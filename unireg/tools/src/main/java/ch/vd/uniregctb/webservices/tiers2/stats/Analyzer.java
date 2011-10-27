package ch.vd.uniregctb.webservices.tiers2.stats;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.HtmlUtils;

class Analyzer {

	private final List<Analyze> analyzes = new ArrayList<Analyze>();
	private final Set<String> methods = new HashSet<String>();

	public void registerAnalyze(Analyze a) {
		analyzes.add(a);
	}

	public void addCall(Call call) {
		for (Analyze analyze : analyzes) {
			methods.add(call.getMethod());
			analyze.addCall(call);
		}
	}

	public void print() {
		for (Analyze analyze : analyzes) {
			analyze.print();
			System.out.println();
		}
	}

	public void printHtml(String htmlFile, boolean localImages) throws IOException {

		if (!htmlFile.toLowerCase().endsWith(".html") && !htmlFile.toLowerCase().endsWith(".htm")) {
			htmlFile += ".html";
		}

		final List<String> list = new ArrayList<String>(methods);
		Collections.sort(list);

		String content = "<html>\n" +
				"  <head>\n" +
				"    <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">\n" +
				"    <title>Temps de réponse du web-service Tiers 2</title>\n" +
				"    <script language=\"javascript\" src=\"http://www.google.com/jsapi\"></script>\n" +
				"  </head>\n" +
				"  <body>\n" +
				"    <h1>Temps de réponse du web-service Tiers 2</h1>\n" +
				"    Les graphiques ci-dessous montrent les statistiques des temps de réponse des appels pour chaque méthode :<br/><br/>\n";

		for (String method : list) {
			for (Analyze analyze : analyzes) {
				final Chart chart = analyze.buildGoogleChart(method);
				if (chart != null) {
					content += "    " + buildChart(method + "_" + analyze.name(), htmlFile, localImages, chart) + "\n";
				}
			}
			content += "    <hr/>";
		}

		content += "    <br/>\n" +
				"    (Dernière mise-à-jour le " + new SimpleDateFormat("dd.MM.yyyy à HH:mm:ss").format(new Date()) + ")\n" +
				"  </body>\n" +
				"</html>";

		final FileWriter writer = new FileWriter(htmlFile);
		writer.write(content);
		writer.close();
	}

	public void analyze(String[] args) {
		try {
			for (String filename : args) {
				analyzeFile(filename);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void analyzeFile(String filename) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String line = reader.readLine();
			while (line != null) {
				process(line);
				line = reader.readLine();
			}

			reader.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Erreur lors du parsing du fichier [" + filename + "]");
		}
	}

	private void process(String line) throws ParseException {

		final Call call = Call.parse(line);
		if (call == null) {
			return;
		}
		addCall(call);
	}

	protected String buildChart(String chartName, String htmlFile, boolean localImages, Chart chart) throws IOException {

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
			final String imagename = dirname + "/" + chartName + ".png";
			InputStream is = null;
			OutputStream os = null;
			try {
				URL u = new URL(chart.getUrl());
				is = u.openStream();
				os = new FileOutputStream(imagename, false);
				FileCopyUtils.copy(is, os);
			}
			catch (IOException e) {
				System.err.println("Exception when connecting to url [" + chart.getUrl() + "] : " + e.getMessage());
				e.printStackTrace();
				return HtmlUtils.htmlEscape(e.getMessage());
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
			final String imageurl = FilenameUtils.getName(dirname) + "/" + chartName + ".png";
			return "<img src=\"" + imageurl + "\" width=\"" + chart.getWidth() + "\" height=\"" + chart.getHeight() + "\" alt=\"" + chartName + "\"/><br/><br/><br/>";
		}
		else {
			return "<img src=\"" + chart.getUrl() + "\" width=\"" + chart.getWidth() + "\" height=\"" + chart.getHeight() + "\" alt=\"" + chartName + "\"/><br/><br/><br/>";
		}
	}
}
