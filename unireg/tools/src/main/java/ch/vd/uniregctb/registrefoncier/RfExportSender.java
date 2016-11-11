package ch.vd.uniregctb.registrefoncier;

import java.io.FileInputStream;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericXmlApplicationContext;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;

/**
 * Cette application permet d'envoyer un fichier d'import du registre foncier (Capitrastra) vers la queue JMS écoutée par Unireg.
 * <p>
 * Exemple d'utilisation :
 * <pre>
 *     RfExportSender -username smx -password smx -env dev -export /home/msi/bidon/unireg/hebdo_import.xml -destination=unireg.rf.import.zsimsn
 * </pre>
 */
public class RfExportSender {

	private static final String ESB_BROKER_URL_INT = "failover:(tcp://esb-broker-ina.etat-de-vaud.ch:50900)";
	private static final String ESB_BROKEN_URL_DEV = "failover:(tcp://esb-dev.etat-de-vaud.ch:60900)";
	private static final String RAFT_URL_INT = "http://raft-in.etat-de-vaud.ch/raft-fs/store";

	private static final Pattern DATED_FILENAME = Pattern.compile(".*([0-9]{8})\\.xml");

	public static void main(String[] args) throws Exception {

		final CommandLine line = parseCommandLine(args);
		if (line == null) {
			// erreur de parsing de la ligne de commande
			System.exit(1);
		}

		final String serviceDestination = line.getOptionValue("destination", "unireg.rf.import");
		final String username = line.getOptionValue("username");
		final String password = line.getOptionValue("password");
		final String env = line.getOptionValue("env");
		final String filename = line.getOptionValue("export");
		final RegDate date = determineDateValeur(line);

		// établissement du context
		final GenericXmlApplicationContext context = new GenericXmlApplicationContext();
		context.registerBeanDefinition("esbUsername", buildStringBean(username));
		context.registerBeanDefinition("esbPassword", buildStringBean(password));
		if (env.equals("int")) {
			context.registerBeanDefinition("esbUrl", buildStringBean(ESB_BROKER_URL_INT));
		}
		else {
			context.registerBeanDefinition("esbUrl", buildStringBean(ESB_BROKEN_URL_DEV));
		}
		context.registerBeanDefinition("raftUrl", buildStringBean(RAFT_URL_INT));
		context.load("classpath:ch/vd/uniregctb/registrefoncier/rf-export-sender.xml");
		context.refresh();
		context.registerShutdownHook();

		System.out.print("Envoi du fichier en cours...");

		// on envoie le fichier
		final EsbJmsTemplate esbTemplate = context.getBean("esbJmsTemplate", EsbJmsTemplate.class);
		try (FileInputStream is = new FileInputStream(filename)) {
			final EsbMessage message = EsbMessageFactory.createMessage();
			message.addAttachment("data", is);
			message.addHeader("dateValeur", RegDateHelper.toIndexString(date));
			message.setBusinessId("rf-export-unireg-" + RegDateHelper.toIndexString(date));
			message.setServiceDestination(serviceDestination);
			message.setBusinessUser("RfExportSender");
			message.setContext("importRF");
			esbTemplate.send(message);
		}

		System.out.println("terminé.");
		context.destroy();
	}

	/**
	 * Détermine la date de valeur du fichier, soit à partir du paramètre '-date', soit à partir du nom du fichier lui-même.
	 */
	@NotNull
	private static RegDate determineDateValeur(CommandLine line) {

		String dateAsString = line.getOptionValue("date");
		if (StringUtils.isBlank(dateAsString)) {
			// on essaie de trouver une date incluse dans le nom du fichier, par exemple : ull_export_20161102.xml -> le 2 novembre 2016
			final Matcher matcher = DATED_FILENAME.matcher(line.getOptionValue("export"));
			if (!matcher.matches()) {
				System.err.println("Le paramètre -date doit être spécifié ou le nom du fichier doit contenir la date selon le format filename_yyyyMMdd.xml");
				System.exit(1);
			}

			dateAsString = matcher.group(1);
		}

		final RegDate date = RegDateHelper.indexStringToDate(dateAsString);
		if (date == null) {
			System.err.println("La date du fichier (" + dateAsString + ") n'est pas une date valide.");
			System.exit(1);
		}

		return date;
	}

	@NotNull
	private static RootBeanDefinition buildStringBean(@Nullable String value) {
		final ConstructorArgumentValues arg = new ConstructorArgumentValues();
		arg.addGenericArgumentValue(StringUtils.trimToEmpty(value));
		return new RootBeanDefinition(String.class, arg, null);
	}

	@SuppressWarnings("static-access")
	private static CommandLine parseCommandLine(String[] args) {

		final CommandLine line;
		try {
			CommandLineParser parser = new GnuParser();
			Option help = new Option("help", "affiche ce message");
			Option username = OptionBuilder.withArgName("username").hasArg().withDescription("nom de l'utilisateur ESB").create("username");
			Option password = OptionBuilder.withArgName("password").hasArg().withDescription("mot-de-passe de l'utilisateur ESB").create("password");
			Option export = OptionBuilder.withArgName("file").hasArg().withDescription("fichier d'import à envoyer").create("export");
			Option date = OptionBuilder.withArgName("yyyyMMdd").hasArg().withDescription("date de valeur du fichier").create("date");
			Option command = OptionBuilder.withArgName("name").hasArg().withDescription("l'environnement de destination (dev|int)").create("env");
			Option destination = OptionBuilder.withArgName("queue").hasArg().withDescription("la queue JMS de destination (défaut: 'unireg.rf.import')").create("destination");

			Options options = new Options();
			options.addOption(help);
			options.addOption(username);
			options.addOption(password);
			options.addOption(export);
			options.addOption(date);
			options.addOption(command);
			options.addOption(destination);

			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || !line.hasOption("username") || !line.hasOption("password") || !line.hasOption("export") || !line.hasOption("env")) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("RfExportSender [options]", "Options:", options, null);
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
