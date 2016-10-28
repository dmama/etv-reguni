package ch.vd.uniregctb.registrefoncier;

import java.io.FileInputStream;
import java.util.Date;

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

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;

/**
 * Cette application permet d'envoyer un fichier d'import du registre foncier (Capitrastra) vers la queue JMS écoutée par Unireg.
 *
 * Exemple d'utilisation :
 * <pre>
 *     RfExportSender -username smx -password smx -env dev -export /home/msi/bidon/unireg/hebdo_import.xml -destination=unireg.rf.import.zsimsn
 * </pre>
 */
public class RfExportSender {

	private static final String ESB_BROKER_URL_INT = "failover:(tcp://esb-broker-ina.etat-de-vaud.ch:50900)";
	private static final String ESB_BROKEN_URL_DEV = "failover:(tcp://esb-dev.etat-de-vaud.ch:60900)";
	private static final String RAFT_URL_INT = "http://raft-in.etat-de-vaud.ch/raft-fs/store";

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
			message.setBusinessId("rf-export-unireg-" + new Date().getTime());
			message.setServiceDestination(serviceDestination);
			message.setBusinessUser("RfExportSender");
			message.setContext("importRF");
			esbTemplate.send(message);
		}

		System.out.println("terminé.");
		context.destroy();
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
			Option command = OptionBuilder.withArgName("name").hasArg().withDescription("l'environnement de destination (dev|int)").create("env");
			Option destination = OptionBuilder.withArgName("queue").hasArg().withDescription("la queue JMS de destination (défaut: 'unireg.rf.import')").create("destination");

			Options options = new Options();
			options.addOption(help);
			options.addOption(username);
			options.addOption(password);
			options.addOption(export);
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
