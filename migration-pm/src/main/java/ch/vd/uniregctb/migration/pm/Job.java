package ch.vd.uniregctb.migration.pm;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.InvariantReloadingStrategy;
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

		// on le met dans l'environnement afin que le PreferencesPlaceholderConfigurer le prenne en compte...
		System.setProperty("ch.vd.unireg.migration.pm.conf", args[0]);

		// chargement du contexte spring
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring/properties.xml",
		                                                                                  "classpath:spring/interfaces.xml",
		                                                                                  "classpath:spring/migration.xml",
		                                                                                  "classpath:spring/database.xml",
		                                                                                  "classpath:spring/regpm.xml");
		context.registerShutdownHook();

		// rechargement des propriétés pour dump dans les logs
		final PropertiesConfiguration properties = new PropertiesConfiguration();
		properties.setReloadingStrategy(new InvariantReloadingStrategy());
		properties.setEncoding("UTF-8");
		properties.setListDelimiter((char) 0); // Disabled
		// Fais le chargement a la fin quand on a disabled le delimiter
		properties.setFileName(args[0]);
		properties.load();

		// Dump des properties
		dumpProperties(properties);

		// !! la suite se passe dans le thread créé par la classe Migrator !!
	}

	private static void dumpSyntax() {
		System.err.println("Le job prend un paramètre qui correspond au chemin d'accès au fichier de configuration.");
	}

	private static void dumpProperties(PropertiesConfiguration props) {
		final Pattern pwdPattern = Pattern.compile("\\bpassword\\b", Pattern.CASE_INSENSITIVE);
		final StringBuilder b = new StringBuilder("Dump des propriétés du traitement :").append(System.lineSeparator());
		final Iterable<String> iterableNames = props::getKeys;
		b.append(StreamSupport.stream(iterableNames.spliterator(), false)
				         .sorted()
				         .map(key -> String.format("\t%s -> %s", key, pwdPattern.matcher(key).find() ? "*******" : enquote(props.getString(key))))
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
