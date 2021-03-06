package ch.vd.unireg.webservices.party3.perfs;

import javax.xml.ws.BindingProvider;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import ch.vd.unireg.perfs.PerfsAccessFile;
import ch.vd.unireg.perfs.PerfsAccessFileIterator;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.PartyWebServiceFactory;

import static ch.vd.unireg.webservices.party3.perfs.PerfsThread.GetQuery;
import static ch.vd.unireg.webservices.party3.perfs.PerfsThread.Query;
import static ch.vd.unireg.webservices.party3.perfs.PerfsThread.SearchQuery;

/**
 * Application de test des performances du web-service Tiers v3 de Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PerfsClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(PerfsClient.class);

	public static final long NANO_TO_MILLI = 1000000;

	private static PerfsClient client;

	private final PerfsAccessFile accessFile;
	private final Integer queriesCount;
	private final int threadCount;
	private final PartyWebService service;

	private final Query query;
	private final Integer batch;

	private ArrayList<PerfsThread> threads;
	private long startTime;

	public static void main(String[] args) {

		final CommandLine line = parseCommandLine(args);
		if (line == null) {
			return;
		}

		final String serviceUrl = (String) line.getArgList().get(0);
		final String username = line.getOptionValue("username");
		final String password = line.getOptionValue("password");
		final String queriesCountAsString = line.getOptionValue("queries");
		final Integer threadsCount = Integer.valueOf(line.getOptionValue("threads", "1"));
		final String ctbIdAsString = line.getOptionValue("ctb");
		final String accessFilename = line.getOptionValue("accessFile");
		final String outputFilename = line.getOptionValue("outputFile");
		final boolean get = line.hasOption("get");
		final String[] partsAsString = line.getOptionValues("parts");
		final Integer batch = Integer.valueOf(line.getOptionValue("batch", "1"));
		final String operateur = line.getOptionValue("operateur", "PerfsClient");
		final Integer oid = Integer.valueOf(line.getOptionValue("oid", "22"));
		final boolean search = line.hasOption("search");

		if (ctbIdAsString == null && accessFilename == null) {
			System.err.println("Une des deux options 'ctb' ou 'accessFile' doit être spécifiée.");
			System.exit(1);
		}

		if (!get && !search) {
			System.err.println("Une des deux options 'get' ou 'search' doit être spécifiée.");
			System.exit(1);
		}

		try {
			final Integer queriesCount;
			if (queriesCountAsString != null) {
				queriesCount = Integer.valueOf(queriesCountAsString);
			}
			else {
				queriesCount = null;
			}

			final FileWriter outputFile = openOutputFile(outputFilename);
			try {
				final Query query;
				if (get) {
					query = new GetQuery(operateur, oid, outputFile);
				}
				else {
					query = new SearchQuery(operateur, oid, outputFile);
				}

				if (partsAsString != null) {
					final Set<PartyPart> parts = EnumSet.noneOf(PartyPart.class);
					for (String s : partsAsString) {
						if (s.equals("ALL")) {
							Collections.addAll(parts, PartyPart.values());
							break;
						}
						PartyPart e = PartyPart.fromValue(s);
						parts.add(e);
					}
					query.setParts(parts);
				}

				if (ctbIdAsString != null) {
					Long id = Long.valueOf(ctbIdAsString);
					client = new PerfsClient(serviceUrl, username, password, query, queriesCount, threadsCount, id);
				}
				else {
					client = new PerfsClient(serviceUrl, username, password, query, queriesCount, threadsCount, accessFilename, batch);
				}

				// Enregistre un shutdown hook de manière à afficher les stats même lorsque l'application est interrompue avec un Ctlr-C.
				Runtime.getRuntime().addShutdownHook(new Thread(PerfsClient::shutdown));

				client.run();
			}
			finally {
				if (outputFile != null) {
					outputFile.close();
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private static void shutdown() {
		client.logStats();
	}

	public PerfsClient(String serviceUrl, String username, String password, Query query, Integer queriesCount, int threadCount, long ctbId) throws Exception {
		LOGGER.info("Démarrage du test");
		LOGGER.info(" - url           = " + serviceUrl);
		LOGGER.info(" - query         = {" + query.description() + '}');
		if (queriesCount != null) {
			LOGGER.info(" - count         = " + queriesCount);
		}
		LOGGER.info(" - threads       = " + threadCount);
		LOGGER.info(" - ctbId         = " + ctbId);

		this.query = query;
		this.batch = null; // par définition, pour un seul tiers
		this.queriesCount = queriesCount;
		this.threadCount = threadCount;
		this.accessFile = new PerfsAccessFile(ctbId);
		this.service = initWebService(serviceUrl, username, password);
	}

	public PerfsClient(String serviceUrl, String username, String password, Query query, Integer queriesCount, int threadCount,
	                   String accessFilename, Integer batch) throws Exception {
		LOGGER.info("Démarrage du test");
		LOGGER.info(" - url     = " + serviceUrl);
		LOGGER.info(" - query   = {" + query.description() + '}');
		if (queriesCount != null) {
			LOGGER.info(" - count   = " + queriesCount);
		}
		if (batch != null) {
			LOGGER.info(" - batch   = " + batch);
		}
		LOGGER.info(" - threads = " + threadCount);
		LOGGER.info(" - accessFile = " + accessFilename);

		this.query = query;
		this.batch = batch;
		this.queriesCount = queriesCount;
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

		final PerfsAccessFileIterator iter;
		if (queriesCount == null) {
			iter = new PerfsAccessFileIterator(accessFile);
		}
		else {
			iter = new PerfsAccessFileIterator(accessFile, queriesCount);
		}

		// Crée les threads
		threads = new ArrayList<>();
		for (int i = 0; i < threadCount; ++i) {
			PerfsThread thread = new PerfsThread(service, query, batch, iter);
			threads.add(thread);
		}

		// Démarre les threads
		startTime = System.nanoTime();
		for (int i = 0; i < threadCount; ++i) {
			PerfsThread thread = threads.get(i);
			thread.start();
		}

		// Attend la fin de l'exécution des threads
		for (int i = 0; i < threadCount; ++i) {
			PerfsThread thread = threads.get(i);
			try {
				thread.join();
			}
			catch (Exception e) {
				LOGGER.warn(e.getMessage(), e);
			}
		}
	}

	private void logStats() {

		long queryTime = 0;
		int queryCount = 0;
		int tiersCount = 0;
		int errorsCount = 0;
		for (int i = 0; i < threadCount; ++i) {
			PerfsThread thread = threads.get(i);
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
			Option ctb = OptionBuilder.withArgName("id").hasArg().withDescription("numéro du contribuable à récupérer").create("ctb");
			Option accessFile = OptionBuilder.withArgName("filename").hasArg().withDescription(
					"fichier avec les numéros des contribuables et les temps d'accès (format host-interface)").create("accessFile");
			Option outputFile = OptionBuilder.withArgName("filename").hasArg().withDescription(
					"fichier de sortie avec le log des données récupérées").create("outputFile");
			Option get = new Option("get", "retourne un tiers avec tout son historique");
			Option parts = OptionBuilder
					.withArgName("name")
					.hasArg()
					.withDescription(
							"spécifie une partie à retourner (voir enum PartyPart). Peut être spécifié plusieurs fois (ou ALL pour toutes les parties).")
					.create("parts");
			Option batch = OptionBuilder.withArgName("size").hasArg().withDescription(
					"si spécifié, récupère les tiers par batch en demandant 'size' tiers à la fois").create("batch");
			Option queries = OptionBuilder.withArgName("count").hasArg().withDescription(
					"nombre de requêtes (défaut=1 ou tous les contribuables du fichier accessFile)").create("queries");
			Option username = OptionBuilder.withArgName("username").hasArg().withDescription(
					"nom de l'utilisateur si le web-service est protégé").create("username");
			Option password = OptionBuilder.withArgName("password").hasArg().withDescription(
					"mot-de-passe de l'utilisateur si le web-service est protégé").create("password");
			Option threads = OptionBuilder.withArgName("count").hasArg().withDescription("nombre de threads (défaut=1)").create("threads");
			Option operateur = OptionBuilder.withArgName("login").hasArg().withDescription(
					"nom de l'opérateur (défaut=PerfsClient)").create("operateur");
			Option oid = OptionBuilder.withArgName("oid").hasArg().withDescription(
					"office d'impôt de l'opérateur (défaut=22)").create("oid");
			Option search = new Option("search", "récupère le nom et prénom d'un tiers puis effecture une recherche avec ces critères");

			Options options = new Options();
			options.addOption(help);
			options.addOption(ctb);
			options.addOption(accessFile);
			options.addOption(outputFile);
			options.addOption(get);
			options.addOption(parts);
			options.addOption(batch);
			options.addOption(queries);
			options.addOption(username);
			options.addOption(password);
			options.addOption(threads);
			options.addOption(operateur);
			options.addOption(oid);
			options.addOption(search);

			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || line.getArgs().length != 1) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("PerfsClient [options] web-service-ul", "Options:", options, null);
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
