package ch.vd.uniregctb.webservices.tiers2.perfs;

import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
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
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.util.ResourceUtils;

import ch.vd.uniregctb.perfs.PerfsAccessFile;
import ch.vd.uniregctb.perfs.PerfsAccessFileIterator;
import ch.vd.uniregctb.webservices.tiers2.Date;
import ch.vd.uniregctb.webservices.tiers2.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.TiersPort;
import ch.vd.uniregctb.webservices.tiers2.TiersService;
import ch.vd.uniregctb.webservices.tiers2.perfs.PerfsThread.DateQuery;
import ch.vd.uniregctb.webservices.tiers2.perfs.PerfsThread.HistoQuery;
import ch.vd.uniregctb.webservices.tiers2.perfs.PerfsThread.PeriodeQuery;
import ch.vd.uniregctb.webservices.tiers2.perfs.PerfsThread.Query;
import ch.vd.uniregctb.webservices.tiers2.perfs.PerfsThread.SearchQuery;

/**
 * Application de test des performances du web-service Tiers de Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PerfsClient {

	private static final Logger LOGGER = Logger.getLogger(PerfsClient.class);

	public static final long NANO_TO_MILLI = 1000000;

	private static PerfsClient client;

	private final PerfsAccessFile accessFile;
	private final Integer queriesCount;
	private final int threadCount;
	private final TiersPort service;

	private final Query query;
	private final Integer batch;

	private ArrayList<PerfsThread> threads;
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
		final String queriesCountAsString = line.getOptionValue("queries");
		final Integer threadsCount = Integer.valueOf(line.getOptionValue("threads", "1"));
		final String ctbIdAsString = line.getOptionValue("ctb");
		final String accessFilename = line.getOptionValue("accessFile");
		final String outputFilename = line.getOptionValue("outputFile");
		final String dateAsString = line.getOptionValue("date");
		final String periodeAsString = line.getOptionValue("periode");
		final boolean histo = line.hasOption("histo");
		final String[] partsAsString = line.getOptionValues("parts");
		final Integer batch = Integer.valueOf(line.getOptionValue("batch", "1"));
		final String operateur = line.getOptionValue("operateur", "PerfsClient");
		final Integer oid = Integer.valueOf(line.getOptionValue("oid", "22"));
		final boolean search = line.hasOption("search");

		if (ctbIdAsString == null && accessFilename == null) {
			System.err.println("Une des deux options 'ctb' ou 'accessFile' doit être spécifiée.");
			System.exit(1);
		}

		if (dateAsString == null && periodeAsString == null && !histo && !search) {
			System.err.println("Une des quatre options 'date', 'periode', 'histo' ou 'search' doit être spécifiée.");
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

			final Query query;
			if (dateAsString != null) {
				Date date = parseDate(dateAsString);
				query = new DateQuery(date, operateur, oid, outputFile);
			}
			else if (periodeAsString != null) {
				int annee = Integer.valueOf(periodeAsString);
				query = new PeriodeQuery(annee, operateur, oid, outputFile);
			}
			else if (histo) {
				query = new HistoQuery(operateur, oid, outputFile);
			}
			else {
				query = new SearchQuery(operateur, oid, outputFile);
			}

			if (partsAsString != null) {
				final Set<TiersPart> parts = new HashSet<TiersPart>();
				for (String s : partsAsString) {
					if (s.equals("ALL")) {
						Collections.addAll(parts, TiersPart.values());
						break;
					}
					TiersPart e = TiersPart.fromValue(s);
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
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					shutdown();
				}
			});

			client.run();

			if (outputFile != null) {
				outputFile.close();
			}
		}
		catch (Exception e) {
			LOGGER.error(e, e);
		}
	}

	private static void shutdown() {
		client.logStats();
	}

	private static Date parseDate(final String dateAsString) throws java.text.ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		java.util.Date dd = format.parse(dateAsString);
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(dd);
		Date date = new Date();
		date.setDay(cal.get(Calendar.DAY_OF_MONTH));
		date.setMonth(cal.get(Calendar.MONTH) + 1);
		date.setYear(cal.get(Calendar.YEAR));
		return date;
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

	private static TiersPort initWebService(String serviceUrl, String username, String password) throws Exception {
		URL wsdlUrl = ResourceUtils.getURL("classpath:TiersService2.wsdl");
		TiersService ts = new TiersService(wsdlUrl);
		TiersPort service = ts.getTiersPortPort();
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
		threads = new ArrayList<PerfsThread>();
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
			Option date = OptionBuilder.withArgName("yyyymmdd").hasArg().withDescription("retourne un tiers à une date précise").create(
					"date");
			Option periode = OptionBuilder.withArgName("annee").hasArg().withDescription("retourne un tiers pour une période fiscale")
					.create("periode");
			Option histo = new Option("histo", "retourne un tiers avec tout son historique");
			Option parts = OptionBuilder
					.withArgName("name")
					.hasArg()
					.withDescription(
							"spécifie une partie à retourner (voir enum TiersPart). Peut être spécifié plusieurs fois (ou ALL pour toutes les parties).")
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
			options.addOption(date);
			options.addOption(periode);
			options.addOption(histo);
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
