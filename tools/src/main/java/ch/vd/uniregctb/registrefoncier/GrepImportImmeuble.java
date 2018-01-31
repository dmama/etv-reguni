package ch.vd.uniregctb.registrefoncier;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.ResourceUtils;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.uniregctb.registrefoncier.dataimport.FichierImmeublesRFParser;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRFImpl;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

/**
 * Cet utilitaire permet de rechercher les éléments qui matchent un pattern et de les imprimer dans la console.
 */
public class GrepImportImmeuble {

	private final XmlHelperRF xmlHelper;
	private final FichierImmeublesRFParser parser;

	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			System.out.println("Usage : GrepImportImmeuble <pattern> <file>");
			System.exit(1);
		}

		GrepImportImmeuble grep = new GrepImportImmeuble();
		grep.run(args);
		System.exit(0);
	}

	public GrepImportImmeuble() throws JAXBException {
		xmlHelper = new XmlHelperRFImpl();
		parser = new FichierImmeublesRFParser();
		parser.setXmlHelperRF(xmlHelper);
	}

	private void run(String[] args) throws IOException, JAXBException, XMLStreamException {

		final Pattern pattern = Pattern.compile(args[0], Pattern.DOTALL);
		final String filename = args[1];

		final File file = ResourceUtils.getFile("file:" + filename);
		assertNotNull(file);

		final MutableInt immeubleCount = new MutableInt(0);
		final MutableInt droitCount = new MutableInt(0);
		final MutableInt proprietaireCount = new MutableInt(0);
		final MutableInt batimentCount = new MutableInt(0);
		final MutableInt surfaceCount = new MutableInt(0);

		final long start = System.nanoTime();

		// on parse le fichier
		final FichierImmeublesRFParser.Callback callback = new FichierImmeublesRFParser.Callback() {
			@Override
			public void onImmeuble(@NotNull Grundstueck immeuble) {
				String xml = toXMLString(immeuble);
				if (pattern.matcher(xml).find()) {
					System.out.println(xml);
				}
				immeubleCount.increment();
			}

			@Override
			public void onDroit(EigentumAnteil droit) {
				String xml = toXMLString(droit);
				if (pattern.matcher(xml).find()) {
					System.out.println(xml);
				}
				droitCount.increment();
			}

			@Override
			public void onProprietaire(@NotNull Personstamm personne) {
				String xml = toXMLString(personne);
				if (pattern.matcher(xml).find()) {
					System.out.println(xml);
				}
				proprietaireCount.increment();
			}

			@Override
			public void onBatiment(@NotNull Gebaeude batiment) {
				String xml = toXMLString(batiment);
				if (pattern.matcher(xml).find()) {
					System.out.println(xml);
				}
				batimentCount.increment();
			}

			@Override
			public void onSurface(@NotNull Bodenbedeckung surface) {
				String xml = toXMLString(surface);
				if (pattern.matcher(xml).find()) {
					System.out.println(xml);
				}
				surfaceCount.increment();
			}

			@Override
			public void done() {
			}
		};

		if (FilenameUtils.isExtension(file.getPath(), "zip")) {
			try (FileInputStream fis = new FileInputStream(file);
			     ZipInputStream zis = new ZipInputStream(fis)) {
				zis.getNextEntry();
				parser.processFile(zis, callback);
			}
		}
		else {
			try (InputStream is = new FileInputStream(file)) {
				parser.processFile(is, callback);
			}
		}

		final long end = System.nanoTime();
		System.out.println("Temps d'exécution: " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms");
	}

	private String toXMLString(Grundstueck obj) {
		return xmlHelper.toXMLString(obj);
	}

	private String toXMLString(EigentumAnteil obj) {
		return xmlHelper.toXMLString(obj);
	}

	private String toXMLString(Personstamm obj) {
		return xmlHelper.toXMLString(obj);
	}

	private String toXMLString(Gebaeude obj) {
		return xmlHelper.toXMLString(obj);
	}

	private String toXMLString(Bodenbedeckung obj) {
		return xmlHelper.toXMLString(obj);
	}
}
