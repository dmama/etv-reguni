package ch.vd.uniregctb.etatcivilcomp;

import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.util.ResourceUtils;

import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.PartyWebServiceFactory;
import ch.vd.uniregctb.perfs.PerfsAccessFile;
import ch.vd.uniregctb.perfs.PerfsAccessFileIterator;

import static ch.vd.uniregctb.etatcivilcomp.EtatCivilCompThread.CompareQuery;
import static ch.vd.uniregctb.etatcivilcomp.EtatCivilCompThread.Query;

/**
 * Application de comparaison des états-civils / pseudo états-civils des appartenances ménage des tiers d'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EtatCivilCompClient {

	private static final Logger LOGGER = Logger.getLogger(EtatCivilCompClient.class);

	public static final long NANO_TO_MILLI = 1000000;

	private static EtatCivilCompClient client;

	private final PerfsAccessFile accessFile;
	private final int threadCount;
	private final PartyWebService service;

	private final Query query;

	private ArrayList<EtatCivilCompThread> threads;
	private long startTime;

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
			DOMConfigurator.configure("src/main/resources/log4j.xml");
		}

		final String serviceUrl = (String) line.getArgList().get(0);
		final String username = line.getOptionValue("username");
		final String password = line.getOptionValue("password");
		final Integer threadsCount = Integer.valueOf(line.getOptionValue("threads", "1"));
		final String accessFilename = line.getOptionValue("accessFile");
		final String outputFilename = line.getOptionValue("outputFile");
		final String operateur = line.getOptionValue("operateur", "PerfsClient");
		final Integer oid = Integer.valueOf(line.getOptionValue("oid", "22"));

		if (accessFilename == null) {
			System.err.println("Le paramètre 'accessFile' doit être spécifié.");
			System.exit(1);
		}

		try {
			final FileWriter outputFile = openOutputFile(outputFilename);

			final CompareQuery query = new CompareQuery(operateur, oid, outputFile);
			client = new EtatCivilCompClient(serviceUrl, username, password, query, threadsCount, accessFilename);

			// Enregistre un shutdown hook de manière à afficher les stats même lorsque l'application est interrompue avec un Ctlr-C.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					shutdown();
				}
			});

			client.run();
		}
		catch (Exception e) {
			LOGGER.error(e, e);
		}
	}

	private static void shutdown() {
		client.logStats();
	}

	public EtatCivilCompClient(String serviceUrl, String username, String password, Query query, int threadCount,
	                           String accessFilename) throws Exception {
		LOGGER.info("Démarrage du test");
		LOGGER.info(" - url     = " + serviceUrl);
		LOGGER.info(" - query   = {" + query.description() + '}');
		LOGGER.info(" - threads = " + threadCount);
		LOGGER.info(" - accessFile = " + accessFilename);

		this.query = query;
		this.threadCount = threadCount;
		this.accessFile = new PerfsAccessFile(accessFilename);
		this.service = initWebService(serviceUrl, username, password);
	}

	private static PartyWebService initWebService(String serviceUrl, String username, String password) throws Exception {
		URL wsdlUrl = ResourceUtils.getURL("classpath:PartyService3.wsdl");
		PartyWebServiceFactory ts = new PartyWebServiceFactory(wsdlUrl);
		PartyWebService service = ts.getService();
		Map<String, Object> context = ((BindingProvider) service).getRequestContext();
		if (StringUtils.isNotBlank(username)) {
			context.put(BindingProvider.USERNAME_PROPERTY, username);
			context.put(BindingProvider.PASSWORD_PROPERTY, password);
		}
		context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceUrl);

		// Désactive la validation du schéma (= ignore silencieusement les éléments inconnus), de manière à permettre l'évolution ascendante-compatible du WSDL.
		context.put(Message.SCHEMA_VALIDATION_ENABLED, false);
		context.put("set-jaxb-validation-event-handler", false);

		return service;
	}

	private static FileWriter openOutputFile(String outputFilename) {
		if (StringUtils.isBlank(outputFilename)) {
			return null;
		}
		try {
			return new FileWriter(outputFilename, false);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void run() {

		final PerfsAccessFileIterator iter = new PerfsAccessFileIterator(accessFile);

		// Crée les threads
		threads = new ArrayList<EtatCivilCompThread>();
		for (int i = 0; i < threadCount; ++i) {
			EtatCivilCompThread thread = new EtatCivilCompThread(service, query, iter);
			threads.add(thread);
		}

		// Démarre les threads
		startTime = System.nanoTime();
		for (int i = 0; i < threadCount; ++i) {
			EtatCivilCompThread thread = threads.get(i);
			thread.start();
		}

		// Attend la fin de l'exécution des threads
		for (int i = 0; i < threadCount; ++i) {
			EtatCivilCompThread thread = threads.get(i);
			try {
				thread.join();
			}
			catch (Exception e) {
				LOGGER.warn(e, e);
			}
		}
	}

	private void logStats() {

		long queryTime = 0;
		int queryCount = 0;
		int tiersCount = 0;
		int errorsCount = 0;
		for (int i = 0; i < threadCount; ++i) {
			EtatCivilCompThread thread = threads.get(i);
			queryTime += thread.getQueryTime();
			queryCount += thread.getQueryCount();
			tiersCount += thread.getTiersCount();
			errorsCount += thread.getErrorsCount();
		}
		queryTime /= NANO_TO_MILLI;

		// Affiche les statistiques
		long time = (System.nanoTime() - startTime) / NANO_TO_MILLI;
		LOGGER.info("Nombre de requêtes     : " + queryCount);
		LOGGER.info("Nombre de tiers        : " + tiersCount);
		LOGGER.info("Nombre de threads      : " + threadCount);
		LOGGER.info("Nombre d'erreurs       : " + errorsCount);
		LOGGER.info("Temps d'exécution réel : " + time + " ms");
		LOGGER.info("Temps d'exécution ws   : " + queryTime + " ms");
		LOGGER.info(" - bande passante      : " + queryCount * 1000 / queryTime + " requêtes/secondes");
		LOGGER.info("                       : " + tiersCount * 1000 / queryTime + " tiers/secondes");
		if (queryCount > 0) {
			LOGGER.info(" - ping moyen          : " + queryTime / queryCount + " ms/requête");
			LOGGER.info("                       : " + queryTime / tiersCount + " ms/tiers");
		}
		else {
			LOGGER.info(" - ping moyen          : <na>");
		}
	}

	@SuppressWarnings({"static-access", "AccessStaticViaInstance"})
	private static CommandLine parseCommandLine(String[] args) {

		final CommandLine line;
		try {
			CommandLineParser parser = new GnuParser();
			Option help = new Option("help", "affiche ce message");
			Option accessFile = OptionBuilder.withArgName("filename").hasArg().withDescription(
					"fichier avec les numéros des contribuables et les temps d'accès (format host-interface)").create("accessFile");
			Option outputFile = OptionBuilder.withArgName("filename").hasArg().withDescription(
					"fichier de sortie avec le log des données récupérées").create("outputFile");
			Option username = OptionBuilder.withArgName("username").hasArg().withDescription(
					"nom de l'utilisateur si le web-service est protégé").create("username");
			Option password = OptionBuilder.withArgName("password").hasArg().withDescription(
					"mot-de-passe de l'utilisateur si le web-service est protégé").create("password");
			Option threads = OptionBuilder.withArgName("count").hasArg().withDescription("nombre de threads (défaut=1)").create("threads");
			Option operateur = OptionBuilder.withArgName("login").hasArg().withDescription(
					"nom de l'opérateur (défaut=PerfsClient)").create("operateur");
			Option oid = OptionBuilder.withArgName("oid").hasArg().withDescription(
					"office d'impôt de l'opérateur (défaut=22)").create("oid");

			Options options = new Options();
			options.addOption(help);
			options.addOption(accessFile);
			options.addOption(outputFile);
			options.addOption(username);
			options.addOption(password);
			options.addOption(threads);
			options.addOption(operateur);
			options.addOption(oid);

			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || line.getArgs().length != 1) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("EtatCivilCompClient [options] web-service-ul", "Options:", options, null);
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
