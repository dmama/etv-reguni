package ch.vd.uniregctb.webservices.tiers2.stats;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

/**
 * Application qui permet d'analyser les fichiers de log d'accès des web-services d'Unireg et qui génère des statistiques sous forme texte ou graphique.
 */
public class WsAccessAnalyzer {

	public static void main(String[] args) throws Exception {

		final CommandLine commandLine = parseCommandLine(args);
		if (commandLine == null) {
			return;
		}

		final String htmlFile = commandLine.getOptionValue("html");
		final boolean localImages = commandLine.hasOption("localImages");
		final String[] files = commandLine.getArgs();
		final String proxy = commandLine.getOptionValue("proxy");
		final boolean timeline = commandLine.hasOption("timeline");
		final boolean distribution = commandLine.hasOption("distribution");

		if (!timeline && !distribution) {
			System.err.println("At least one of '-distribution' and '-timeline' parameters must be specified.");
			return;
		}

		if (StringUtils.isNotBlank(proxy)) {
			final String[] p = proxy.split(":");
			System.setProperty("http.proxyHost", p[0]);
			System.setProperty("http.proxyPort", p[1]);
		}

		final Analyzer analyzer = new Analyzer();
		if (timeline) {
			analyzer.registerAnalyze(new TimelineAnalyze());
		}
		if (distribution) {
			analyzer.registerAnalyze(new DistributionAnalyze());
		}
		analyzer.analyze(files);

		if (StringUtils.isNotBlank(htmlFile)) {
			analyzer.printHtml(htmlFile, localImages);
		}
		else {
			analyzer.print();
		}

	}

	@SuppressWarnings({"static-access", "AccessStaticViaInstance"})
	private static CommandLine parseCommandLine(String[] args) {

		final CommandLine line;
		try {
			CommandLineParser parser = new GnuParser();
			Option help = new Option("help", "display this help");
			Option html = OptionBuilder.withArgName("file").hasArg().withDescription("output results in a html file").create("html");
			Option distribution = new Option("distribution", "analyzes data and outputs the distribution of response times");
			Option timeline = new Option("timeline", "analyzes data and outputs the timeline of response times");
			Option localImages = new Option("localImages", "store images in local folder (with -html)");
			Option proxy = OptionBuilder.withArgName("host:port").hasArg().withDescription("use HTTP proxy on given port").create("proxy");

			Options options = new Options();
			options.addOption(help);
			options.addOption(html);
			options.addOption(timeline);
			options.addOption(distribution);
			options.addOption(localImages);
			options.addOption(proxy);

			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || line.getArgs().length == 0) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("WsAccessAnalyzer [options] FILE...", "Options:", options, null);
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
