package ch.vd.uniregctb.migration.pm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Classe principale pour la migration
 */
public class Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(Job.class);

	/**
	 * Méthode d'entrée du batch
	 *
	 * @param args les paramètres de la ligne de commande
	 */
	public static void main(String[] args) throws Exception {

		// le seul paramètre sur la ligne de commande est le chemin vers un fichier de propriétés
		if (args == null || args.length != 1) {
			dumpSyntax();
			return;
		}

		initLog4j();

		// chargement des propriétés
		final String propertiesPath = args[0];
		final File propertiesFile = new File(propertiesPath);
		final Properties props = loadProperties(propertiesFile);
		dumpProperties(props);

		// on le met dans l'environnement afin que le PreferencesPlaceholderConfigurer le prenne en compte...
		System.setProperty("ch.vd.unireg.migration.pm.conf", args[0]);

		// chargement du contexte spring
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring/properties.xml",
		                                                                                  "classpath:spring/migration.xml",
		                                                                                  "classpath:spring/regpm.xml");
		context.registerShutdownHook();

		// !! la suite se passe dans le thread créé par la classe Migrator !!
	}

	private static void dumpSyntax() {
		System.err.println("Le job prend un paramètre qui correspond au chemin d'accès au fichier de configuration.");
	}

	private static void dumpProperties(Properties props) {
		final Pattern pwdPattern = Pattern.compile("\\bpassword\\b", Pattern.CASE_INSENSITIVE);
		final List<String> propertyNames = new ArrayList<>(props.stringPropertyNames());
		Collections.sort(propertyNames);
		final StringBuilder b = new StringBuilder("Dump des propriétés du traitement :").append(System.lineSeparator());
		for (String key : propertyNames) {
			final Matcher matcher = pwdPattern.matcher(key);
			final String value;
			if (matcher.find()) {
				value = "********";
			}
			else {
				value = enquote(props.getProperty(key));
			}
			b.append(String.format("\t%s -> %s", key, value)).append(System.lineSeparator());
		}
		LOGGER.info(b.toString());
	}

	private static String enquote(String str) {
		if (str == null) {
			return "null";
		}
		return String.format("'%s'", str);
	}

	private static Properties loadProperties(File file) throws IOException {
		try (InputStream is = new FileInputStream(file); Reader r = new InputStreamReader(is, "UTF-8")) {
			final Properties props = new Properties();
			props.load(r);
			return props;
		}
	}

	/**
	 * Initialise Log4j
	 */
	private static void initLog4j() {
		final Properties properties = new Properties();
		properties.setProperty("log4j.logger.ch.vd.uniregctb", "DEBUG");
		properties.setProperty("log4j.rootLogger", "ERROR, stdout");
		properties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		properties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
		properties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%-5.5p [%8.8t] [%d{yyyy-MM-dd HH:mm:ss.SSS}] %m%n");
		PropertyConfigurator.configure(properties);

		// Ces deux classes semblent avoir l'oreille un peu dure...
		java.util.logging.Logger l = java.util.logging.Logger.getLogger("org.apache.cxf.bus.spring.BusApplicationContext");
		l.setLevel(Level.WARNING);
		l = java.util.logging.Logger.getLogger("org.apache.cxf.service.factory.ReflectionServiceFactoryBean");
		l.setLevel(Level.WARNING);
	}
}
