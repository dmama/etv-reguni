package ch.vd.uniregctb.webservices.tiers.compare;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.util.ResourceUtils;

import ch.vd.interfaces.fiscal.Fiscal;
import ch.vd.interfaces.fiscal.FiscalService;
import ch.vd.interfaces.fiscal.RechercherNoContribuable;

/**
 * Application de test des performances du web-serviceUnireg Tiers de Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>, Baba Ngom <baba-issa.ngom@vd.ch>
 */

public class CompareClient {
	private static final Logger LOGGER = Logger.getLogger(CompareClient.class);

	public static final long NANO_TO_MILLI = 1000000;


	private final Fiscal serviceUnireg;
	private final Fiscal serviceHost;
	private final List<RechercherNoContribuable> ListeRecherche;

	public static void main(String[] args) {

		final CommandLine line = parseCommandLine(args);
		if (line == null) {
			return;
		}

		if (new File("log4j.xml").exists()) {
			// run depuis ant
			DOMConfigurator.configure("log4j.xml");
		}
		else {
			// run depuis éclipse
			DOMConfigurator.configure("src/main/java/ch/vd/uniregctb/webservices/tiers/log4j.xml");
		}

		final String serviceU = (String) line.getArgList().get(0);
		final String serviceH = (String) line.getArgList().get(1);
		final String usernameU = line.getOptionValue("usernameU");
		final String passwordU = line.getOptionValue("passwordU");
		final String usernameH = line.getOptionValue("usernameH");
		final String passwordH = line.getOptionValue("passwordH");
		final String accessFilename = line.getOptionValue("accessFile");




		if (accessFilename == null) {
			System.err.println("'accessFile' doit être spécifiée.");
			System.exit(1);
		}


		try {

			final CompareClient client;

			client = new CompareClient(serviceU, usernameU, passwordU, serviceH, usernameH, passwordH, accessFilename);

			client.run();
		}
		catch (Exception e) {
			LOGGER.error(e, e);
		}
	}

	public CompareClient(String serviceUrlUnireg, String usernameU, String passwordU, String serviceUrlHost, String usernameH,
			String passwordH, String accessFilename) throws Exception {
		LOGGER.info("Demarrage de la comparaison");
		LOGGER.info(" - url ws Unireg    = " + serviceUrlUnireg);
		LOGGER.info(" - url ws Host    = " + serviceUrlHost);

		LOGGER.info(" - accessFile = " + accessFilename);

		this.serviceUnireg = initWebService(serviceUrlUnireg, usernameU, passwordU);
		this.serviceHost = initWebService(serviceUrlHost, usernameH, passwordH);
		this.ListeRecherche = ContribuableFileReader.chargerContribuable(accessFilename);
	}

	private static Fiscal initWebService(String serviceUrl, String username, String password) throws Exception {
		URL wsdlUrl = ResourceUtils.getURL("classpath:FiscalService.wsdl");
		FiscalService fs = new FiscalService(wsdlUrl);
		Fiscal service = fs.getFiscalServicePort();
		Map<String, Object> context = ((BindingProvider) service).getRequestContext();
		if (StringUtils.isNotBlank(username)) {
			context.put(BindingProvider.USERNAME_PROPERTY, username);
			context.put(BindingProvider.PASSWORD_PROPERTY, password);
		}
		context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceUrl);
		return service;
	}

	public void run() {

		// Crée les threads
		for (RechercherNoContribuable rechercheIt : ListeRecherche) {
			CompareThread thread = new CompareThread(serviceUnireg, serviceHost, rechercheIt);
			thread.start();
		}

	}

	@SuppressWarnings("static-access")
	private static CommandLine parseCommandLine(String[] args) {

		final CommandLine line;
		try {
			CommandLineParser parser = new GnuParser();
			Option help = new Option("help", "affiche ce message");
			Option accessFile = OptionBuilder.withArgName("filename").hasArg().withDescription(
					"fichier avec les numéros des contribuables et les temps d'accès (format host-interface)").create("accessFile");
			Option usernameU = OptionBuilder.withArgName("<usernameU>").hasArg().withDescription(
					"nom de l'utilisateur si le web-service Unireg fiscal est protégé)").create("usernameU");
			Option passwordU = OptionBuilder.withArgName("<passwordU>").hasArg().withDescription(
					"mot-de-passe de l'utilisateur si le web-service Unireg fiscal est protégé)").create("passwordU");
			Option usernameH = OptionBuilder.withArgName("<usernameH>").hasArg().withDescription(
					"nom de l'utilisateur si le web-service Host est protégé)").create("usernameH");
			Option passwordH = OptionBuilder.withArgName("<passwordH>").hasArg().withDescription(
					"mot-de-passe de l'utilisateur si le web-service Host est protégé)").create("passwordH");



			Options options = new Options();
			options.addOption(help);
			options.addOption(accessFile);
			options.addOption(usernameU);
			options.addOption(passwordU);
			options.addOption(usernameH);
			options.addOption(passwordH);


			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || line.getArgs().length != 2) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("CompareClient [options] web-serviceUnireg-url web-serviceHost-url", "Options:", options, null);
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
