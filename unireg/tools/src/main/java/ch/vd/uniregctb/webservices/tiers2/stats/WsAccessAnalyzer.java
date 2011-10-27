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
		final boolean parts = commandLine.hasOption("parts");
		final boolean load = commandLine.hasOption("load");

		if (!timeline && !distribution && !parts && !load) {
			System.err.println("At least one of '-load', '-distribution', '-timeline' or '-parts' parameters must be specified.");
			return;
		}

		if (StringUtils.isNotBlank(proxy)) {
			final String[] p = proxy.split(":");
			System.setProperty("http.proxyHost", p[0]);
			System.setProperty("http.proxyPort", p[1]);
		}

		final Analyzer analyzer = new Analyzer();
		if (load) {
			analyzer.registerAnalyze(new LoadAnalyze());
		}
		if (timeline) {
			analyzer.registerAnalyze(new TimelineAnalyze(true));
			analyzer.registerAnalyze(new TimelineAnalyze(false));
		}
		if (distribution) {
			analyzer.registerAnalyze(new DistributionAnalyze());
		}
		if (parts) {
			analyzer.registerAnalyze(new PartsAnalyze());
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
			Option load = new Option("load", "analyzes data and outputs load");
			Option distribution = new Option("distribution", "analyzes data and outputs the distribution of response times");
			Option timeline = new Option("timeline", "analyzes data and outputs the timeline of response times");
			Option parts = new Option("parts", "analyzes data and outputs parts usage");
			Option localImages = new Option("localImages", "store images in local folder (with -html)");
			Option proxy = OptionBuilder.withArgName("host:port").hasArg().withDescription("use HTTP proxy on given port").create("proxy");

			Options options = new Options();
			options.addOption(help);
			options.addOption(html);
			options.addOption(load);
			options.addOption(timeline);
			options.addOption(distribution);
			options.addOption(parts);
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
			System.err.println("Parsing error : " + exp.getMessage());
			return null;
		}

		return line;

	}
}
