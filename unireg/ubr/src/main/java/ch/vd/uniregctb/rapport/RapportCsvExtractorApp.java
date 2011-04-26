package ch.vd.uniregctb.rapport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.PropertyConfigurator;

import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfString;

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

		if (!command.equals("list") && (csvfiles == null || csvfiles.length == 0)) {
			System.err.println("Le nom du ou des fichiers csv à extraire doit être spécifié. Utiliser l'option '-help' pour plus d'information.");
			System.exit(1);
		}

		if (!command.equals("list") && outputdir == null) {
			System.err.println("Le répertoire de destination des extractions doit être spécifié. Utiliser l'option '-help' pour plus d'information.");
			System.exit(1);
		}

		if (command.equals("list")) {
			listeFichiersInclus(pdffile);
		}
		else if (command.equals("extract")) {
			extractionFichiersInclus(pdffile, csvfiles, outputdir);
		}
		else {
			System.err.println("Commande '" + command + "' inconnue. Utiliser l'option '-help' pour plus d'information.");
			System.exit(1);
		}

		System.exit(0);
	}

	private static void listeFichiersInclus(String pdffile) {
		try {
			final PdfReader reader = ouvrirFichierPdf(pdffile);
			boucleSurFichiers(PdfDictionary.class, reader, new Traitement<PdfDictionary>() {
				public void traite(PdfDictionary element) {
					if (PdfName.FILESPEC.equals(element.get(PdfName.TYPE))) {
						System.out.println(element.getAsString(PdfName.F));
					}
				}
			});
		}
		catch (IOException ex) {
			System.err.println("Erreur d'accès au fichier pdf : " + ex.getMessage());
			System.exit(1);
		}
	}

	private static void extractionFichiersInclus(String pdffiles, String[] cvsfiles, String outputdir) {

		try {
			final PdfReader reader = ouvrirFichierPdf(pdffiles);

			// on remplit d'abord la liste des noms
			final List<PdfString> noms = new ArrayList<PdfString>();
			boucleSurFichiers(PdfDictionary.class, reader, new Traitement<PdfDictionary>() {
				public void traite(PdfDictionary element) {
					if (PdfName.FILESPEC.equals(element.get(PdfName.TYPE))) {
						noms.add(element.getAsString(PdfName.F));
					}
				}
			});

			// puis on va chercher les documents qui vont bien
			final File outputdirFile = new File(outputdir);
			if (!outputdirFile.exists()) {
				if (!outputdirFile.mkdirs()) {
					System.err.println("Impossible de créer le répertoire de destination des fichiers extraits");
					System.exit(1);
				}
			}

			final Set<String> cvsAExtraire = new HashSet<String>(Arrays.asList(cvsfiles));
			final MutableInt index = new MutableInt(0);
			boucleSurFichiers(PRStream.class, reader, new Traitement<PRStream>() {
				public void traite(PRStream element) {
					if (PdfName.EMBEDDEDFILE.equals(element.get(PdfName.TYPE))) {
						final String nomFichierExtrait = noms.get(index.intValue()).toString();
						if (cvsAExtraire.contains(nomFichierExtrait)) {
							try {
								final byte[] content = PdfReader.getStreamBytes(element);
								final File file = new File(outputdirFile, nomFichierExtrait);
								final FileOutputStream stream = new FileOutputStream(file);
								try {
									stream.write(content);
								}
								finally {
									stream.close();
								}
							}
							catch (IOException ex) {
								System.err.println("Erreur lors de l'écriture du fichier extrait " + nomFichierExtrait + " : " + ex.getMessage());
								System.exit(1);
							}
						}
						index.increment();
					}
				}
			});
		}
		catch (IOException ex) {
			System.err.println("Erreur d'accès au fichier pdf : " + ex.getMessage());
			System.exit(1);
		}
	}

	private static PdfReader ouvrirFichierPdf(String pdffile) throws IOException {
		final File fileName = new File(pdffile);
		final PdfReader reader = new PdfReader(new FileInputStream(fileName));
		return reader;
	}

	private static interface Traitement<T extends PdfObject> {
		void traite(T element);
	}

	@SuppressWarnings("unchecked")
	private static <T extends PdfObject> void boucleSurFichiers(Class<T> clazz, PdfReader reader, Traitement<T> aFaire) {
		final int nbFiles = reader.getXrefSize();
		for (int i = 0 ; i < nbFiles ; ++ i) {
			final PdfObject object = PdfReader.getPdfObject(reader.getPdfObject(i));
			if (object != null && clazz.isAssignableFrom(object.getClass())) {
				aFaire.traite((T) object);
			}
		}
	}

	/**
	 * Initialise Log4j de manière à ce qu'il soit le plus discret possible.
	 */
	private static void initLog4j() {
		Properties properties = new Properties();
		properties.setProperty("log4j.rootLogger", "ERROR, stdout");
		properties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		properties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
		properties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%r %p [%t] %c - %m%n");
		PropertyConfigurator.configure(properties);
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
					"noms des fichiers csv à extraire du rapport").create("csvfiles");

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
