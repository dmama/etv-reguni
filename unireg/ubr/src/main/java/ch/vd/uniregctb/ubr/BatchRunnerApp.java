package ch.vd.uniregctb.ubr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

import ch.vd.uniregctb.webservices.batch.BatchWSException;
import ch.vd.uniregctb.webservices.batch.JobDefinition;
import ch.vd.uniregctb.webservices.batch.JobSynchronousMode;
import ch.vd.uniregctb.webservices.batch.Param;
import ch.vd.uniregctb.webservices.batch.Report;

/**
 * Application qui permet de piloter les batches d'Unireg à partir de la ligne de commande.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class BatchRunnerApp {

	private static final String FILE_PREFIX = "file:" + File.separatorChar + File.separatorChar;
	private static final String N_A = "-";

	public static void main(String[] args) throws Exception {

		initLog4j();

		final CommandLine line = parseCommandLine(args);
		if (line == null) {
			return;
		}

		final String serviceUrl;
		final String username;
		final String password;

		final String config = line.getOptionValue("config");
		if (config != null) {
			// [UNIREG-1332] si spécifié, le fichier de configuration contient les paramètres de connexion
			Properties props = new Properties();

			FileInputStream in = null;
			try {
				in = new FileInputStream(config);
				props.load(in);
			}
			finally {
				if (in != null) {
					in.close();
				}
			}

			serviceUrl = props.getProperty("url");
			username = props.getProperty("username");
			password = props.getProperty("password");
		}
		else {
			// Autrement, les paramètres de connexion doivent être spécifié sur la ligne de commande
			serviceUrl = (String) line.getArgList().get(0);
			username = line.getOptionValue("username");
			password = line.getOptionValue("password");
		}

		final String name = line.getOptionValue("batch");
		final String command = line.getOptionValue("command");
		final String outputDir = line.getOptionValue("outputDir");
		final String[] params = line.getOptionValues("params");

		if (command == null) {
			System.err.println("La commande doit être spécifiée.");
			System.exit(1);
		}

		if (!command.equals("list") && name == null) {
			System.err.println("Le nom du batch doit être spécifié.");
			System.exit(1);
		}

		BatchRunnerClient client = new BatchRunnerClient(serviceUrl, username, password);

		if (command.equals("start")) {
			startBatch(name, params, client);
		}
		else if (command.equals("run")) {
			runBatch(name, params, client);
		}
		else if (command.equals("stop")) {
			stopBatch(name, client);
		}
		else if (command.equals("list")) {
			final List<String> names = client.getBatchNames();
			for (String s : names) {
				System.out.println(s);
			}
		}
		else if (command.equals("show")) {
			showBatch(name, client);
		}
		else if (command.equals("status")) {
			statusBatch(name, client);
		}
		else if (command.equals("lastreport")) {
			lastReport(name, outputDir, client);
		}

		System.exit(0);
		return;
	}

	/**
	 * Initialise Log4j de manière à ce qu'il soit le plus discret possible.
	 */
	private static void initLog4j() {
		Properties properties = new Properties();
		properties.setProperty("log4j.logger.ch.vd.uniregctb", "WARN");
		properties.setProperty("log4j.rootLogger", "ERROR, stdout");
		properties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		properties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
		properties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "[ubr] %p [%d{yyyy-MM-dd HH:mm:ss.SSS}] %c | %m%n");
		PropertyConfigurator.configure(properties);

		// Ces deux classes semblent avoir l'oreille un peu dur...
		java.util.logging.Logger l = java.util.logging.Logger.getLogger("org.apache.cxf.bus.spring.BusApplicationContext");
		l.setLevel(Level.WARNING);
		l = java.util.logging.Logger.getLogger("org.apache.cxf.service.factory.ReflectionServiceFactoryBean");
		l.setLevel(Level.WARNING);
	}

	private static void startBatch(String name, String[] params, BatchRunnerClient client) throws BatchWSException {
		client.startBatch(name, array2map(params));
	}

	private static void runBatch(String name, String[] params, BatchRunnerClient client) throws BatchWSException {
		client.runBatch(name, array2map(params));
	}

	private static void stopBatch(String name, BatchRunnerClient client) throws BatchWSException {
		client.stopBatch(name);
	}

	private static void statusBatch(String name, BatchRunnerClient client) {
		final JobDefinition def = client.getBatchDefinition(name);
		if (def == null) {
			System.err.println("batch " + name + " doesn't exists.");

		}
		else {
			System.out.println(def.getStatut().name());
		}
	}

	private static void showBatch(String name, BatchRunnerClient client) {
		final JobDefinition def = client.getBatchDefinition(name);
		if (def == null) {
			System.err.println("batch " + name + " doesn't exists.");

		}
		else {
			System.out.print("name:            ");
			System.out.println(def.getName());
			System.out.print("description:     ");
			System.out.println(def.getDescription());
			System.out.print("parameters:      ");
			toString(def.getParams());
			System.out.print("execution mode:  ");
			System.out.println(toString(def.getSynchronousMode()));
			System.out.print("last stop:       ");
			System.out.println(toString(def.getLastEnd()));
			System.out.print("last start:      ");
			System.out.println(toString(def.getLastStart()));
			System.out.print("running message: ");
			System.out.println(toString(def.getRunningMessage()));
			System.out.print("status:          ");
			System.out.println(def.getStatut().name());
		}
	}

	/**
	 * Récupère le dernier rapport d'exécution du batch spécifié et sauve-le dans le répertoire de sortie indiqué.
	 *
	 * @param name
	 *            le nom du batch
	 * @param outputDir
	 *            le répertoire de sortie
	 */
	private static void lastReport(String name, String outputDir, BatchRunnerClient client) throws IOException, BatchWSException {

		// récupère le rapport
		final Report report = client.getLastReport(name);
		if (report == null) {
			System.err.println("no last report found for batch " + name + ".");
			return;
		}

		// sauve le rapport
		final String filename = addPath(outputDir, report.getFileName());

		InputStream is = null;
		FileOutputStream os = null;
		try {
			is = report.getContentByteStream().getInputStream();
			os = new FileOutputStream(filename);
			FileCopyUtils.copy(is, os);
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException e) {
					// ignored
				}
			}
			if (os != null) {
				try {
					os.close();
				}
				catch (IOException e) {
					// ignored
				}
			}
		}

		System.out.print("name:        ");
		System.out.println(report.getName());
		System.out.print("description: ");
		System.out.println(report.getDescription());
		System.out.print("filePath:    ");
		System.out.println(filename);
	}

	/**
	 * Ajoute un chemin à un autre, en ajoutant - si nécessaire - le caractère de séparation.
	 *
	 * @param left
	 *            chemin de gauche
	 * @param right
	 *            chemin de droite
	 * @return chemin résultant
	 */
	private static String addPath(String left, String right) {
		if (left == null || left.equals("")) {
			return right;
		}
		if (left.charAt(left.length() - 1) == File.separatorChar) {
			return left + right;
		}
		else {
			return left + File.separatorChar + right;
		}
	}

	private static class ParamLine {
		public final String name;
		public final String type;
		public final String enumValues;
		public final String defaultValue;

		public ParamLine(String name, String type, String enumValues, String defaultValue) {
			this.name = name;
			this.type = type;
			this.enumValues = enumValues;
			this.defaultValue = defaultValue;
		}

		public ParamLine(Param p) {
			this.name = p.getName();
			this.type = p.getType();

			final List<String> v = p.getEnumValues();
			this.enumValues = (v == null || v.isEmpty()) ? N_A : ArrayUtils.toString(v.toArray());

			final String d = p.getDefaultValue();
			this.defaultValue = d == null ? N_A : d;
		}

		public void println(String format) {
			final String line = String.format(format, name, type, enumValues, defaultValue);
			System.out.println(line);
		}
	}

	private static void toString(List<Param> pl) {
		if (pl == null || pl.isEmpty()) {
			System.out.println(N_A);
		}
		else {
			System.out.println("");

			List<ParamLine> lines = new ArrayList<ParamLine>(pl.size() + 1);
			lines.add(new ParamLine("name", "type", "enum values", "default"));

			int maxName = 0;
			int maxType = 0;
			int maxEnum = 0;
			int maxDefault = 0;

			for (Param p : pl) {
				final ParamLine line = new ParamLine(p);
				lines.add(line);

				maxName = Math.max(maxName, line.name.length());
				maxType = Math.max(maxType, line.type.length());
				maxEnum = Math.max(maxEnum, line.enumValues.length());
				maxDefault = Math.max(maxDefault, line.defaultValue.length());
			}

			final String format = "%#" + (maxName + 8) + "s | %#" + (maxType + 2) + "s | %#" + maxEnum + "s | %#" + (maxDefault + 2) + "s";

			for (int i = 0; i < lines.size(); ++i) {
				final ParamLine line = lines.get(i);
				line.println(format);
				if (i == 0) {
					System.out.println("      " + fillString('-', maxName + maxType + maxEnum + maxDefault + 19));
				}
			}
		}
	}

	private static String fillString(char c, int i) {
		char[] fill = new char[i];
		Arrays.fill(fill, c);
		return new String(fill);
	}

	private static String toString(final String message) {
		return message == null ? N_A : message;
	}

	private static String toString(JobSynchronousMode mode) {
		return mode == null ? N_A : mode.name();
	}

	private static String toString(final XMLGregorianCalendar lastEnd) {
		if (lastEnd == null) {
			return N_A;
		}
		Date date = lastEnd.toGregorianCalendar().getTime();
		return date.toString();
	}

	/**
	 * Converti un array de paramètres (indexes pairs = clé, indexes impairs = valeur) en une map.
	 * <p>
	 * <b>Note:</b> si la valeur d'un paramètres commence par <i>file://</i>, le contenu du fichier pointé est transmis (à utiliser dans le
	 * cas de paramètre de type fichiers, évidemment).
	 */
	private static HashMap<String, Object> array2map(final String[] params) {
		final HashMap<String, Object> map = new HashMap<String, Object>();
		if (params != null) {
			for (String param : params) {
				// on splitte les paramètres "key:value" à la main, pour gérer correctement le cas où la valeur est "file://qqch"
				final int pos = param.indexOf(':');
				if (pos == -1) {
					throw new IllegalArgumentException("Erreur: le paramètre ["+param+"] ne possède pas de valeur associée.");
				}

				final String key = param.substring(0, pos);
				final String stringValue = param.substring(pos + 1);
				final Object value;
				if (stringValue.startsWith(FILE_PREFIX)) {
					// paramète de type fichier
					try {
						File file = ResourceUtils.getFile(stringValue);
						byte[] bytes = FileUtils.readFileToByteArray(file);
						value = bytes;
					}
					catch (FileNotFoundException e) {
						throw new RuntimeException(e);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				else {
					// paramètre normal
					value = stringValue;
				}
				map.put(key, value);
			}
		}
		return map;
	}

	@SuppressWarnings("static-access")
	private static CommandLine parseCommandLine(String[] args) {

		final CommandLine line;
		try {
			CommandLineParser parser = new GnuParser();
			Option help = new Option("help", "affiche ce message");
			Option username = OptionBuilder.withArgName("username").hasArg().withDescription(
					"nom de l'utilisateur si le web-service est protégé").create("username");
			Option password = OptionBuilder.withArgName("password").hasArg().withDescription(
					"mot-de-passe de l'utilisateur si le web-service est protégé").create("password");
			Option config = OptionBuilder.withArgName("file").hasArg().withDescription(
					"fichier de propriétés permettant de spécifier les valeurs suivantes : username, password et url").create("config");
			Option command = OptionBuilder.withArgName("command").hasArg().withDescription(
					"la commande à effectuer (start|run|stop|list|show|status|lastreport)").create("command");
			Option batch = OptionBuilder.withArgName("batch").hasArg().withDescription("le nom du batch à piloter").create("batch");
			Option outputDir = OptionBuilder.withArgName("path").hasArg().withDescription(
					"le répertoire de téléchargement des rapports (uniquement avec command=lastreport)").create("outputDir");
			Option params = OptionBuilder.withArgName("key:value [key:value ...]").hasArgs().withDescription(
					"paramètres de démarrage du batch").create("params");

			Options options = new Options();
			options.addOption(help);
			options.addOption(username);
			options.addOption(password);
			options.addOption(config);
			options.addOption(command);
			options.addOption(batch);
			options.addOption(outputDir);
			options.addOption(params);

			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || (!line.hasOption("config") && line.getArgs().length != 1)) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("BatchRunner [url] [options]", "Options:", options, null);
				return null;
			}
		}
		catch (ParseException exp) {
			System.err.println("Erreur de parsing.  Raison: " + exp.getMessage());
			return null;
		}

		return line;
	}
}
