package ch.vd.uniregctb.migration.pm;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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
	 * Les modes d'exécution autorisés
	 */
	private static final Set<MigrationMode> ALLOWED_MODES = EnumSet.of(MigrationMode.DIRECT, MigrationMode.DUMP, MigrationMode.FROM_DUMP);

	/**
	 * Méthode d'entrée du batch
	 *
	 * @param args les paramètres de la ligne de commandes
	 */
	public static void main(String[] args) throws Exception {

		// les paramètres sur la ligne de commandes sont
		// 1. le mode d'exécution
		// 2+. les chemins vers les fichiers de propriétés
		if (args == null || args.length < 2) {
			dumpSyntax();
			System.exit(1);
		}

		// extraction du mode d'exécution
		final MigrationMode mode;
		try {
			mode = MigrationMode.valueOf(args[0]);
			if (!ALLOWED_MODES.contains(mode)) {
				throw new IllegalArgumentException();
			}
		}
		catch (IllegalArgumentException e) {
			System.err.println("Mauvaise valeur du mode d'exécution ('" + args[0] + "') !");
			dumpSyntax();
			System.exit(2);
			return;         // jamais exécuté, mais pour être sûr qu'Idea comprenne bien que la variable "mode" a toujours une valeur....
		}

		// on met les chemins des fichiers de configuration dans l'environnement afin que cela puisse être pris en compte
		// dans le PreferencesPlaceholderConfigurer
		final int nbFichiersConfiguration = args.length - 1;
		System.setProperty("ch.vd.unireg.migration.pm.conf.nb", Integer.toString(nbFichiersConfiguration));
		for (int i = 0 ; i < nbFichiersConfiguration ; ++ i) {
			System.setProperty("ch.vd.unireg.migration.pm.conf.file." + i, args[i + 1]);
		}

		// construction de la liste des fichiers spring à charger
		final String[] locations = buildSpringContextLocations(mode);

		// chargement du contexte spring
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(locations);
		context.registerShutdownHook();

		// rechargement des propriétés pour dump dans les logs
		final Properties properties = new Properties();
		for (String arg : Arrays.asList(args).subList(1, args.length)) {
			try (InputStream is = new FileInputStream(arg); Reader r = new InputStreamReader(is, "UTF-8")) {
				properties.load(r);
			}
		}

		// Dump des properties
		dumpProperties(properties);

		// !! la suite se passe dans le thread créé par la classe Migrator !!
	}

	private static String[] buildSpringContextLocations(MigrationMode mode) {
		// construction de la liste des fichiers spring à charger
		final List<String> fichiersContexteSpring = new ArrayList<>(10);

		// dans tous les cas : les propriétés
		fichiersContexteSpring.add("classpath:spring/properties.xml");

		// partie de migration vers Unireg
		if (mode == MigrationMode.DIRECT || mode == MigrationMode.FROM_DUMP) {
			fichiersContexteSpring.add("classpath:spring/interfaces.xml");
			fichiersContexteSpring.add("classpath:spring/migration.xml");
			fichiersContexteSpring.add("classpath:spring/database.xml");
			fichiersContexteSpring.add("classpath:spring/validation.xml");
			fichiersContexteSpring.add("classpath:spring/services.xml");
			fichiersContexteSpring.add("classpath:spring/jmx-migration.xml");
		}

		// partie de la récupération des données dans le mainframe
		if (mode == MigrationMode.DIRECT || mode == MigrationMode.DUMP) {
			fichiersContexteSpring.add("classpath:spring/regpm.xml");
		}

		// fichiers spécifiques à chaque mode
		switch (mode) {
		case DUMP:
			fichiersContexteSpring.add("classpath:spring/migration-dump.xml");
			fichiersContexteSpring.add("classpath:spring/jmx-dump.xml");
			break;
		case DIRECT:
			fichiersContexteSpring.add("classpath:spring/migration-direct.xml");
			break;
		case FROM_DUMP:
			fichiersContexteSpring.add("classpath:spring/migration-from-dump.xml");
			break;
		default:
			// le contexte ne sera vraissemblablement pas complet...
			break;
		}

		// transformation en tableau et retour
		return fichiersContexteSpring.toArray(new String[fichiersContexteSpring.size()]);
	}

	private static void dumpSyntax() {
		System.err.println("Erreur dans les paramètres.");
		System.err.println("Le premier paramètre doit correspondre au mode d'exécution : " + Arrays.toString(ALLOWED_MODES.toArray(new MigrationMode[ALLOWED_MODES.size()])));
		System.err.println("Les paramètres suivants correspondent aux chemins d'accès vers les fichiers de configuration.");
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
