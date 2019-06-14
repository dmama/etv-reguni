package ch.vd.unireg.ubr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Application qui permet de piloter les batches d'Unireg à partir de la ligne de commande.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class BatchRunnerApp {

	private static final String FILE_PREFIX = "file:" + File.separatorChar + File.separatorChar;
	private static final String N_A = "-";

	private abstract static class BatchException extends Exception {
		protected final String batchName;

		protected BatchException(String batchName) {
			this.batchName = batchName;
		}

		@Override
		public abstract String getMessage();
	}

	private static final class UnknownBatchException extends BatchException {
		private UnknownBatchException(String batchName) {
			super(batchName);
		}

		@Override
		public String getMessage() {
			return String.format("Batch %s does not exist.", batchName);
		}
	}

	private static final class NoLastReportFoundException extends BatchException {
		private NoLastReportFoundException(String batchName) {
			super(batchName);
		}

		@Override
		public String getMessage() {
			return String.format("No last report found for batch %s.", batchName);
		}
	}

	public static void main(String[] args) throws Exception {

		initLog4j();

		final CommandLine line = parseCommandLine(args);
		if (line == null) {
			// erreur de parsing de la ligne de commande
			System.exit(1);
		}

		final String serviceUrl;
		final String username;
		final String password;

		final String config = line.getOptionValue("config");
		if (config != null) {
			// [UNIREG-1332] si spécifié, le fichier de configuration contient les paramètres de connexion
			final Properties props = new Properties();
			try (FileInputStream in = new FileInputStream(config)) {
				props.load(in);
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
			System.err.println("Command should be specified.");
			System.exit(1);
		}

		if (!command.equals("list") && !command.equals("list-details") && name == null) {
			System.err.println("Batch name should be specified.");
			System.exit(1);
		}

		final BatchRunnerClient client = new BatchRunnerClient(serviceUrl, username, password);

		try {
			switch (command) {
			case "start":
				startBatch(name, params, client);
				break;
			case "run":
				runBatch(name, params, client);
				break;
			case "stop":
				stopBatch(name, client);
				break;
			case "list":
				client.getBatchNames().forEach(System.out::println);
				break;
			case "list-details":
				client.getBatchNames().forEach(n -> {
					System.out.println("-----------------------------------------------------------------");
					try {
						printBatchDefinition(n, client, System.out);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					System.out.println();
				});
				break;
			case "show":
				printBatchDefinition(name, client, System.out);
				break;
			case "status":
				statusBatch(name, client);
				break;
			case "lastreport":
				lastReport(name, outputDir, client);
				break;
			default:
				System.err.println("Unknown command '" + command + "'");
				System.exit(1);
			}
		}
		catch (BatchException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

		System.exit(0);
	}

	/**
	 * Initialise Log4j de manière à ce qu'il soit le plus discret possible.
	 */
	private static void initLog4j() {

		final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		final AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
		builder.add(console);

		final LayoutComponentBuilder standard = builder.newLayout("PatternLayout");
		standard.addAttribute("pattern", "[ubr] %p [%d{yyyy-MM-dd HH:mm:ss.SSS}] %c | %m%n");
		console.add(standard);

		final RootLoggerComponentBuilder rootLogger = builder.newRootLogger(org.apache.logging.log4j.Level.ERROR);
		rootLogger.add(builder.newAppenderRef("stdout"));
		builder.add(rootLogger);
		builder.add(builder.newLogger("ch.vd.unireg", Level.WARN));
		builder.add(builder.newLogger("ch.vd.unireg", Level.WARN));

		Configurator.initialize(builder.build());
	}

	private static void startBatch(String name, String[] params, BatchRunnerClient client) throws BatchRunnerClientException {
		client.startBatch(name, array2map(params));
	}

	private static void runBatch(String name, String[] params, BatchRunnerClient client) throws BatchRunnerClientException {
		client.runBatch(name, array2map(params));
	}

	private static void stopBatch(String name, BatchRunnerClient client) throws BatchRunnerClientException {
		client.stopBatch(name);
	}

	private static void statusBatch(String name, BatchRunnerClient client) throws UnknownBatchException, BatchRunnerClientException {
		final JobStatus status = client.getBatchStatus(name);
		if (status == null) {
			throw new UnknownBatchException(name);
		}
		else {
			System.out.println(status.name());
		}
	}

	public static void printBatchDefinition(String name, BatchRunnerClient client, @NotNull PrintStream out) throws UnknownBatchException, BatchRunnerClientException {
		final JobDescription def = client.getBatchDescription(name);
		if (def == null) {
			throw new UnknownBatchException(name);
		}
		else {
			out.print("name:            ");
			out.println(def.getName());
			out.print("description:     ");
			out.println(def.getDescription());
			out.print("parameters:      ");
			printParametersDefinition(def.getParameters(), out);
			out.print("last stop:       ");
			out.println(toString(def.getLastEnd()));
			out.print("last start:      ");
			out.println(toString(def.getLastStart()));
			out.print("running message: ");
			out.println(toString(def.getRunningMessage()));
			out.print("status:          ");
			out.println(def.getStatus().name());
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
	private static void lastReport(String name, String outputDir, BatchRunnerClient client) throws IOException, BatchRunnerClientException, NoLastReportFoundException {

		// récupère le rapport
		try (Report report = client.getLastReport(name)) {
			if (report == null) {
				throw new NoLastReportFoundException(name);
			}

			// sauve le rapport
			final String filename = addPath(outputDir, report.getFileName());
			try (InputStream is = report.getContent(); FileOutputStream os = new FileOutputStream(filename)) {
				IOUtils.copy(is, os);
			}

			System.out.print("filePath: ");
			System.out.println(filename);
		}
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
		if (left == null || left.isEmpty()) {
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
		public final String mandatoryFlag;
		public final String multiValues;
		public final String enumValues;

		public ParamLine(String name, String type, String mandatoryFlag, String multiValues, String enumValues) {
			this.name = name;
			this.type = type;
			this.mandatoryFlag = mandatoryFlag;
			this.multiValues = multiValues;
			this.enumValues = enumValues;
		}

		public ParamLine(JobParamDescription p) {
			this.name = p.getName();
			this.type = p.getType();
			this.mandatoryFlag = p.isMandatory() ? "Y" : "N";
			this.multiValues = p.isMultiValues() ? "Y" : "N";
			this.enumValues = p.getEnumValues() == null || p.getEnumValues().length == 0 ? N_A : ArrayUtils.toString(p.getEnumValues());
		}

		public void println(String format, @NotNull PrintStream out) {
			final String line = String.format(format, name, type, mandatoryFlag, multiValues, enumValues);
			out.println(line);
		}
	}

	private static void printParametersDefinition(@Nullable List<JobParamDescription> pl, @NotNull PrintStream out) {
		if (pl == null || pl.isEmpty()) {
			out.println(N_A);
		}
		else {
			out.println();

			final List<ParamLine> lines = new ArrayList<>(pl.size() + 1);
			lines.add(new ParamLine("name", "type", "mandatory", "multivalues", "enum values"));

			int maxName = "name".length();
			int maxType = "type".length();
			int maxMandatory = "mandatory".length();
			int maxMultivalues = "multivalues".length();
			int maxEnum = "enum values".length();

			for (JobParamDescription p : pl) {
				final ParamLine line = new ParamLine(p);
				lines.add(line);

				maxName = Math.max(maxName, line.name.length());
				maxType = Math.max(maxType, line.type.length());
				maxEnum = Math.max(maxEnum, line.enumValues.length());
			}

			final String format = "%" + (maxName + 8) + "s | %" + (maxType + 2) + "s | %" + maxMandatory + "s | %" + maxMultivalues + "s | %" + maxEnum + 's';

			for (int i = 0; i < lines.size(); ++i) {
				final ParamLine line = lines.get(i);
				line.println(format, out);
				if (i == 0) {
					out.println("      " + fillString('-', maxName + maxType + maxMandatory + maxMultivalues + maxEnum + 18));
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

	private static String toString(final Date date) {
		if (date == null) {
			return N_A;
		}
		return date.toString();
	}

	/**
	 * Converti un array de paramètres (indexes pairs = clé, indexes impairs = valeur) en une map.
	 * <p>
	 * <b>Note:</b> si la valeur d'un paramètres commence par <i>file://</i>, le contenu du fichier pointé est transmis (à utiliser dans le
	 * cas de paramètre de type fichiers, évidemment).
	 */
	private static Map<String, Object> array2map(final String[] params) {
		final Map<String, Object> map = new HashMap<>();
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
						final File file = ResourceUtils.getFile(stringValue);
						value = FileUtils.readFileToByteArray(file);
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
					"la commande à effectuer (start|run|stop|list|list-details|show|status|lastreport)").create("command");
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
				final HelpFormatter formatter = new HelpFormatter();
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
