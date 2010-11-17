package ch.vd.uniregctb.webservices.tiers2.stats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.springframework.util.FileCopyUtils;

abstract class Analyzer {

	public abstract void addCall(String method, HourMinutes timestamp, long millisecondes);

	public abstract void printHtml(String htmlFile, boolean localImages) throws IOException;

	public abstract void print();

	public void analyze(String[] args) {
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

	private void process(String line) {

		try {
			final Call call = Call.parse(line);
			if (call == null) {
				return;
			}
			addCall(call.getMethod(), call.getTimestamp(), call.getMilliseconds() / call.getTiersCount());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected String buildChart(String chartName, String htmlFile, boolean localImages, String chartUrl) throws IOException {

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
				URL u = new URL(chartUrl);
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
			final String imageurl = FilenameUtils.getName(dirname) + "/" + chartName + ".png";
			return "<img src=\"" + imageurl + "\" width=\"1000\" height=\"200\" alt=\"" + chartName + "\"/><br/><br/><br/>";
		}
		else {
			return "<img src=\"" + chartUrl + "\" width=\"1000\" height=\"200\" alt=\"" + chartName + "\"/><br/><br/><br/>";
		}
	}
}
