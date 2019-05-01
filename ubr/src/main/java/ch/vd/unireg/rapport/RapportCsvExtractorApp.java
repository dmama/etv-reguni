package ch.vd.unireg.rapport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfString;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Petit utilitaire qui extrait les fichiers csv inclus dans un rapport PDF généré par UNIREG
 * à la fin de l'exécution d'un batch
 */
public class RapportCsvExtractorApp {

	public static void main(String[] args) {

		initLog4j();

		final CommandLine line = parseCommandLine(args);
		if (line == null) {
			return;
		}

		final String pdffile = (String) line.getArgList().get(0);
		final String command = line.getOptionValue("command");
		final String outputdir = line.getOptionValue("outputdir");
		final String[] csvfiles = line.getOptionValues("csvfiles");

		if (command == null) {
			System.err.println("La commande doit être spécifiée. Utiliser l'option '-help' pour plus d'information.");
			System.exit(1);
		}

		if (!command.equals("list") && outputdir == null) {
			System.err.println("Le répertoire de destination des extractions doit être spécifié. Utiliser l'option '-help' pour plus d'information.");
			System.exit(1);
		}

		switch (command) {
		case "list":
			listeFichiersInclus(pdffile);
			break;
		case "extract":
			extractionFichiersInclus(pdffile, csvfiles, outputdir);
			break;
		default:
			System.err.println("Commande '" + command + "' inconnue. Utiliser l'option '-help' pour plus d'information.");
			System.exit(1);
		}

		System.exit(0);
	}

	/**
	 * Sous-classe du PdfReader pour y ajouter le support de l'interface AutoCloseable
	 * et des méthodes helper pour sortir sous forme de stream les objets inclus dans le PDF
	 */
	private static final class CloseablePdfReader extends PdfReader implements AutoCloseable {
		public CloseablePdfReader(InputStream is) throws IOException {
			super(is);
		}

		public Stream<PdfObject> stream() {
			final int nbFiles = getXrefSize();
			return IntStream.range(0, nbFiles)
					.mapToObj(this::getPdfObject)
					.filter(Objects::nonNull);
		}

		public <T extends PdfObject> Stream<T> stream(Class<T> clazz) {
			return stream()
					.filter(clazz::isInstance)
					.map(clazz::cast);
		}
	}

	private static void listeFichiersInclus(String pdffile) {
		try (final FileInputStream fis = new FileInputStream(pdffile);
		     final CloseablePdfReader reader = new CloseablePdfReader(fis)) {

			reader.stream(PdfDictionary.class)
					.filter(dict -> PdfName.FILESPEC.equals(dict.get(PdfName.TYPE)))
					.map(dict -> dict.getAsString(PdfName.F))
					.forEach(System.out::println);
		}
		catch (IOException ex) {
			System.err.println("Erreur d'accès au fichier pdf : " + ex.getMessage());
			System.exit(1);
		}
	}

	private static void extractionFichiersInclus(String pdffiles, String[] cvsfiles, String outputdir) {

		try (final FileInputStream fis = new FileInputStream(pdffiles);
		     final CloseablePdfReader reader = new CloseablePdfReader(fis)) {

			// on remplit d'abord la liste des noms des fichiers inclus
			final List<PdfString> noms = reader.stream(PdfDictionary.class)
					.filter(dict -> PdfName.FILESPEC.equals(dict.get(PdfName.TYPE)))
					.map(dict -> dict.getAsString(PdfName.F))
					.collect(Collectors.toList());

			// puis on va chercher les documents qui vont bien
			final File outputdirFile = new File(outputdir);
			if (!outputdirFile.exists()) {
				if (!outputdirFile.mkdirs()) {
					System.err.println("Impossible de créer le répertoire de destination des fichiers extraits");
					System.exit(1);
				}
			}

			final Set<String> cvsAExtraire = (cvsfiles == null || cvsfiles.length == 0 ? null : new HashSet<>(Arrays.asList(cvsfiles)));
			final MutableInt index = new MutableInt(0);
			reader.stream(PRStream.class)
					.filter(elt -> PdfName.EMBEDDEDFILE.equals(elt.get(PdfName.TYPE)))
					.forEach(elt -> {
						final String nomFichierExtrait = noms.get(index.intValue()).toString();
						if (cvsAExtraire == null || cvsAExtraire.contains(nomFichierExtrait)) {
							try {
								final byte[] content = PdfReader.getStreamBytes(elt);
								final File file = new File(outputdirFile, nomFichierExtrait);
								try (FileOutputStream stream = new FileOutputStream(file)) {
									stream.write(content);
								}
							}
							catch (IOException ex) {
								System.err.println("Erreur lors de l'écriture du fichier extrait " + nomFichierExtrait + " : " + ex.getMessage());
								System.exit(1);
							}
						}
						index.increment();
					});
		}
		catch (IOException ex) {
			System.err.println("Erreur d'accès au fichier pdf : " + ex.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Initialise Log4j de manière à ce qu'il soit le plus discret possible.
	 */
	private static void initLog4j() {
		final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		final AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
		builder.add(console);

		final LayoutComponentBuilder standard = builder.newLayout("PatternLayout");
		standard.addAttribute("pattern", "%r %p [%t] %c - %m%n");
		console.add(standard);

		final RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.ERROR);
		rootLogger.add(builder.newAppenderRef("stdout"));

		builder.add(rootLogger);

		Configurator.initialize(builder.build());
	}

	@SuppressWarnings("static-access")
	private static CommandLine parseCommandLine(String[] args) {

		final CommandLine line;
		try {
			final GnuParser parser = new GnuParser();
			final Option help = new Option("help", "affiche ce message");

			final Option command = OptionBuilder.withArgName("command").hasArg().withDescription(
					"la commande à effectuer (list|extract)").create("command");
			final Option outputdir = OptionBuilder.withArgName("outputdir").hasArg().withDescription(
					"répertoire de sortie des fichiers cvs extraits du rapport").create("outputdir");
			final Option csvfiles = OptionBuilder.withArgName("toto.csv [titi.csv ...]").hasArgs().withDescription(
					"noms des fichiers csv à extraire du rapport (ne pas renseigner le paramètre pour extraire tous les fichiers)").create("csvfiles");

			final Options options = new Options();
			options.addOption(help);
			options.addOption(command);
			options.addOption(outputdir);
			options.addOption(csvfiles);

			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || line.getArgs().length != 1) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("RapportCsvExtractor fichier.pdf [options]", "Options : ", options, null);
				return null;
			}
		}
		catch (ParseException exp) {
			System.err.println("Erreur de parsing. Raison : " + exp.getMessage());
			return null;
		}

		return line;
	}
}
