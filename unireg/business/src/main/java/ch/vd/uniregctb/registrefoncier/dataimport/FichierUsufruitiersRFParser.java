package ch.vd.uniregctb.registrefoncier.dataimport;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

import org.codehaus.stax2.XMLInputFactory2;
import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.rechteregister.Dienstbarkeit;

/**
 * Classe qui parse un fichier d'import des usufruitiers et bénéficiaires de droits d'habitation du registre foncier et qui notifie au fil du parsing des éléments lus.
 */
public class FichierUsufruitiersRFParser {

	private static final String RECHTREGISTER_NAMESPACE = "http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister";

	private static final String LIST_DROITS = "StandardRechtList";
	private static final String DROIT = "Dienstbarkeit";

	private XmlHelperRF xmlHelperRF;

	public void setXmlHelperRF(XmlHelperRF xmlHelperRF) {
		this.xmlHelperRF = xmlHelperRF;
	}

	/**
	 * Interface orientée-événement pour recevoir les entités au fur et à mesure qu'elles sont parsées.
	 */
	public interface Callback {
		void onDroit(@NotNull Dienstbarkeit droit);
	}

	/**
	 * Parse le fichier spécifié. Les éléments extraits sont immédiatement proposé au processing à travers l'interface de callback.
	 *
	 * @param is       le flux du fichier à parser
	 * @param callback une interface de callback pour recevoir en flux tendu les éléments parsés.
	 * @throws XMLStreamException exception levée si le fichier n'est pas valide XML
	 * @throws JAXBException      exception levée si JAXB n'arrive pas interpréter le fichier XML
	 */
	public void processFile(@NotNull InputStream is, @NotNull Callback callback) throws XMLStreamException, JAXBException {

		final XMLInputFactory2 factory = (XMLInputFactory2) XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
		factory.configureForSpeed();

		final XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(is);
		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				if (!RECHTREGISTER_NAMESPACE.equals(xmlStreamReader.getNamespaceURI())) {
					throw new IllegalArgumentException("Le namespace du fichier n'est pas le bon");
				}
				final String localName = xmlStreamReader.getLocalName();
				switch (localName) {
				case LIST_DROITS:
					processDroits(xmlStreamReader, callback);
					break;
				default:
					xmlStreamReader.next();
				}
			}
			else {
				xmlStreamReader.next();
			}

		}
		xmlStreamReader.close();
	}

	/**
	 * Exemple XML :
	 * <pre>
	 * 	&lt;StandardRechtList&gt;
	 * 		&lt;Dienstbarkeit VersionID="1f109152380ffd8901380ffec2516c10" MasterID="1f109152380ffd8901380ffec2506c02"&gt;
	 * 			&lt;StandardRechtID&gt;_1f109152380ffd8901380ffec2506c02&lt;/StandardRechtID&gt;
	 * 			&lt;BeteiligtesGrundstueckIDREF&gt;_1f109152380ffd8901380ffe0d893e41&lt;/BeteiligtesGrundstueckIDREF&gt;
	 * 			&lt;RechtEintragJahrID&gt;2004&lt;/RechtEintragJahrID&gt;
	 * 			&lt;RechtEintragNummerID&gt;151&lt;/RechtEintragNummerID&gt;
	 * 			&lt;Bereinigungsmarkierung&gt;false&lt;/Bereinigungsmarkierung&gt;
	 * 			&lt;AmtNummer&gt;8&lt;/AmtNummer&gt;
	 * 			&lt;Stichwort&gt;
	 * 				&lt;TextDe&gt;*Habitation&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Droit d'habitation&lt;/TextFr&gt;
	 * 			&lt;/Stichwort&gt;
	 * 			&lt;Beleg&gt;
	 * 				&lt;AmtNummer&gt;8&lt;/AmtNummer&gt;
	 * 				&lt;BelegJahr&gt;2004&lt;/BelegJahr&gt;
	 * 				&lt;BelegNummer&gt;534&lt;/BelegNummer&gt;
	 * 				&lt;BelegNummerIndex&gt;0&lt;/BelegNummerIndex&gt;
	 * 			&lt;/Beleg&gt;
	 * 			&lt;BeginDatum&gt;2004-11-08+01:00&lt;/BeginDatum&gt;
	 * 			&lt;Entschaedigung&gt;0&lt;/Entschaedigung&gt;
	 * 			&lt;Wert&gt;0&lt;/Wert&gt;
	 * 			&lt;Meldungspflichtig&gt;gem_code&lt;/Meldungspflichtig&gt;
	 * 			&lt;Personenberechtigt&gt;true&lt;/Personenberechtigt&gt;
	 * 			&lt;Grundstueckeberechtigt&gt;false&lt;/Grundstueckeberechtigt&gt;
	 * 			&lt;EintragungAlsSdR&gt;false&lt;/EintragungAlsSdR&gt;
	 * 		&lt;/Dienstbarkeit&gt;
	 * 		&lt;Dienstbarkeit VersionID="1f109152380ffd8901380ffefad64374" MasterID="1f109152380ffd8901380ffefad54360"&gt;
	 * 			&lt;StandardRechtID&gt;_1f109152380ffd8901380ffefad54360&lt;/StandardRechtID&gt;
	 * 			&lt;BeteiligtesGrundstueckIDREF&gt;_1f109152380ffd8901380ffe090827e1&lt;/BeteiligtesGrundstueckIDREF&gt;
	 * 			&lt;RechtEintragJahrID&gt;2006&lt;/RechtEintragJahrID&gt;
	 * 			&lt;RechtEintragNummerID&gt;361&lt;/RechtEintragNummerID&gt;
	 * 			&lt;Bereinigungsmarkierung&gt;false&lt;/Bereinigungsmarkierung&gt;
	 * 			&lt;AmtNummer&gt;8&lt;/AmtNummer&gt;
	 * 			&lt;Stichwort&gt;
	 * 				&lt;TextDe&gt;*Usufruit&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Usufruit&lt;/TextFr&gt;
	 * 			&lt;/Stichwort&gt;
	 * 			&lt;Rechtzusatz&gt;conventionnel&lt;/Rechtzusatz&gt;
	 * 			&lt;Beleg&gt;
	 * 				&lt;AmtNummer&gt;8&lt;/AmtNummer&gt;
	 * 				&lt;BelegJahr&gt;2006&lt;/BelegJahr&gt;
	 * 				&lt;BelegNummer&gt;285&lt;/BelegNummer&gt;
	 * 				&lt;BelegNummerIndex&gt;0&lt;/BelegNummerIndex&gt;
	 * 			&lt;/Beleg&gt;
	 * 			&lt;BeginDatum&gt;2006-06-30+02:00&lt;/BeginDatum&gt;
	 * 			&lt;Entschaedigung&gt;0&lt;/Entschaedigung&gt;
	 * 			&lt;Wert&gt;0&lt;/Wert&gt;
	 * 			&lt;Meldungspflichtig&gt;gem_code&lt;/Meldungspflichtig&gt;
	 * 			&lt;Personenberechtigt&gt;true&lt;/Personenberechtigt&gt;
	 * 			&lt;Grundstueckeberechtigt&gt;false&lt;/Grundstueckeberechtigt&gt;
	 * 			&lt;EintragungAlsSdR&gt;false&lt;/EintragungAlsSdR&gt;
	 * 		&lt;/Dienstbarkeit&gt;
	 * 	&lt;/StandardRechtList&gt;
	 * </pre>
	 */
	private void processDroits(XMLStreamReader xmlStreamReader, Callback callback) throws JAXBException, XMLStreamException {

		final Unmarshaller unmarshaller = xmlHelperRF.getAutreDroitContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (DROIT.equals(localName)) {
					final Dienstbarkeit droit = (Dienstbarkeit) unmarshaller.unmarshal(xmlStreamReader);
					if (droit != null) {
						callback.onDroit(droit);
					}
				}
				else {
					// on ignore les autres droits de type Anmerkung, Vormerkung et Grundlast
					xmlStreamReader.next();
				}
			}
			else if (eventType == XMLStreamConstants.END_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (LIST_DROITS.equals(localName)) {
					return; // fin de liste, on sort
				}
				else {
					xmlStreamReader.next();
				}
			}
			else {
				xmlStreamReader.next();
			}
		}

	}
}
