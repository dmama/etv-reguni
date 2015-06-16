package ch.vd.uniregctb.migration.pm;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	 * @param args les paramètres de la ligne de commandes
	 */
	public static void main(String[] args) throws Exception {

		// les paramètres sur la ligne de commandes sont les chemins vers les fichiers de propriétés
		if (args == null || args.length < 1) {
			dumpSyntax();
			return;
		}

		// on met les chemins des fichiers de configuration dans l'environnement afin que cela puisse être pris en compte
		// dans le PreferencesPlaceholderConfigurer
		System.setProperty("ch.vd.unireg.migration.pm.conf.nb", Integer.toString(args.length));
		for (int i = 0 ; i < args.length ; ++ i) {
			System.setProperty("ch.vd.unireg.migration.pm.conf.file." + i, args[i]);
		}

		// chargement du contexte spring
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring/properties.xml",
		                                                                                  "classpath:spring/interfaces.xml",
		                                                                                  "classpath:spring/migration.xml",
		                                                                                  "classpath:spring/database.xml",
		                                                                                  "classpath:spring/validation.xml",
		                                                                                  "classpath:spring/services.xml",
		                                                                                  "classpath:spring/regpm.xml");
		context.registerShutdownHook();

		// rechargement des propriétés pour dump dans les logs
		final Properties properties = new Properties();
		for (String arg : args) {
			try (InputStream is = new FileInputStream(arg); Reader r = new InputStreamReader(is, "UTF-8")) {
				properties.load(r);
			}
		}

		// Dump des properties
		dumpProperties(properties);

		// !! la suite se passe dans le thread créé par la classe Migrator !!
	}

	private static void dumpSyntax() {
		System.err.println("Le job prend des paramètres qui correspondent aux chemins d'accès aux fichiers de configuration.");
	}

	private static void dumpProperties(Properties props) {
		final Pattern usrpwdPattern = Pattern.compile("\\b(user|password)\\b", Pattern.CASE_INSENSITIVE);
		final StringBuilder b = new StringBuilder("Dump des propriétés du traitement :").append(System.lineSeparator());
		b.append(props.stringPropertyNames().stream()
				         .sorted()
				         .map(key -> String.format("\t%s -> %s", key, usrpwdPattern.matcher(key).find() ? "*******" : enquote(props.getProperty(key))))
				         .collect(Collectors.joining(System.lineSeparator())));
		LOGGER.info(b.toString());
	}

	private static String enquote(String str) {
		if (str == null) {
			return "null";
		}
		return String.format("'%s'", str);
	}
}
