package ch.vd.uniregctb.registrefoncier;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.util.ResourceUtils;

import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.uniregctb.registrefoncier.dataimport.FichierUsufruitiersRFParser;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRFImpl;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

/**
 * Cet utilitaire permet de rechercher les éléments qui matchent un pattern et de les imprimer dans la console.
 */
public class GrepImportUsufruits {

	private final XmlHelperRF xmlHelper;
	private final FichierUsufruitiersRFParser parser;

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
		parser = new FichierUsufruitiersRFParser();
		parser.setXmlHelperRF(xmlHelper);
	}

	private void run(String[] args) throws IOException, JAXBException, XMLStreamException {

		final Pattern pattern = Pattern.compile(args[0], Pattern.DOTALL);
		final String filename = args[1];

		final File file = ResourceUtils.getFile("file:" + filename);
		assertNotNull(file);

		final MutableInt droitCount = new MutableInt(0);

		final long start = System.nanoTime();

		// on parse le fichier
		final FichierUsufruitiersRFParser.Callback callback = droit -> {
			String xml = toXMLString(droit);
			if (pattern.matcher(xml).find()) {
				System.out.println(xml);
			}
			droitCount.increment();
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
}
