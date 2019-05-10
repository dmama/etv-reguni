package ch.vd.unireg.registrefoncier;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.ResourceUtils;

import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.Personstamm;
import ch.vd.unireg.registrefoncier.dataimport.FichierServitudeRFParser;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRFImpl;

/**
 * Cet utilitaire permet de rechercher les éléments qui matchent un pattern et de les imprimer dans la console.
 */
public class GrepImportUsufruits {

	private final XmlHelperRF xmlHelper;
	private final FichierServitudeRFParser parser;

	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			System.out.println("Usage : GrepImportUsufruits <pattern> <file>");
			System.exit(1);
		}

		GrepImportUsufruits grep = new GrepImportUsufruits();
		grep.run(args);
		System.exit(0);
	}

	public GrepImportUsufruits() throws JAXBException {
		xmlHelper = new XmlHelperRFImpl();
		parser = new FichierServitudeRFParser();
		parser.setXmlHelperRF(xmlHelper);
	}

	private void run(String[] args) throws IOException, JAXBException, XMLStreamException {

		final Pattern pattern = Pattern.compile(args[0], Pattern.DOTALL);
		final String filename = args[1];

		final File file = ResourceUtils.getFile("file:" + filename);
		if (file == null) {
			throw new IllegalArgumentException();
		}

		final MutableInt servitudeCount = new MutableInt(0);
		final MutableInt groupesBeneficiairesCount = new MutableInt(0);
		final MutableInt beneficiairesCount = new MutableInt(0);

		final long start = System.nanoTime();

		// on parse le fichier
		final FichierServitudeRFParser.Callback callback = new FichierServitudeRFParser.Callback() {
			@Override
			public void onServitude(@NotNull Dienstbarkeit servitude) {
				String xml = toXMLString(servitude);
				if (pattern.matcher(xml).find()) {
					System.out.println(xml);
				}
				servitudeCount.increment();
			}

			@Override
			public void onGroupeBeneficiaires(@NotNull LastRechtGruppe beneficiaires) {
				String xml = toXMLString(beneficiaires);
				if (pattern.matcher(xml).find()) {
					System.out.println(xml);
				}
				groupesBeneficiairesCount.increment();
			}

			@Override
			public void onBeneficiaire(@NotNull Personstamm beneficiaire) {
				String xml = toXMLString(beneficiaire);
				if (pattern.matcher(xml).find()) {
					System.out.println(xml);
				}
				beneficiairesCount.increment();
			}

			@Override
			public void done() {

			}
		};

		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		final long end = System.nanoTime();
		System.out.println("Temps d'exécution: " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms");
	}

	private String toXMLString(Dienstbarkeit obj) {
		return xmlHelper.toXMLString(obj);
	}

	private String toXMLString(LastRechtGruppe obj) {
		return xmlHelper.toXMLString(obj);
	}

	private String toXMLString(Personstamm obj) {
		return xmlHelper.toXMLString(obj);
	}
}
