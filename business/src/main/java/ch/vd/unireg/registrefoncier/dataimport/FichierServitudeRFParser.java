package ch.vd.unireg.registrefoncier.dataimport;

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
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.Personstamm;

/**
 * Classe qui parse un fichier d'import des servitudes et qui notifie au fil du parsing des éléments lus.
 */
public class FichierServitudeRFParser {

	public static final String RECHTREGISTER_NAMESPACE = "http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister";

	private static final String LIST_SERVITUDES = "StandardRechtList";
	private static final String SERVITUDE = "Dienstbarkeit";
	private static final String LIST_GROUPES_BENEFICIAIRES = "LastRechtGruppeList";
	private static final String GROUPE_BENEFICIAIRES = "LastRechtGruppe";
	private static final String LIST_BENEFICIAIRES = "PersonstammList";
	private static final String BENEFICIAIRE_PP = "NatuerlichePersonstamm";
	private static final String BENEFICIAIRE_PM = "JuristischePersonstamm";

	private XmlHelperRF xmlHelperRF;

	public void setXmlHelperRF(XmlHelperRF xmlHelperRF) {
		this.xmlHelperRF = xmlHelperRF;
	}

	/**
	 * Interface orientée-événement pour recevoir les entités au fur et à mesure qu'elles sont parsées.
	 */
	public interface Callback {
		void onServitude(@NotNull Dienstbarkeit servitude);

		void onGroupeBeneficiaires(@NotNull LastRechtGruppe beneficiaires);

		void onBeneficiaire(@NotNull Personstamm beneficiaire);

		/**
		 * Méthode appelée lorsque toutes les données ont été envoyées.
		 */
		void done();
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
				case LIST_SERVITUDES:
					processServitudes(xmlStreamReader, callback);
					break;
				case LIST_GROUPES_BENEFICIAIRES:
					processGroupesBeneficiaires(xmlStreamReader, callback);
					break;
				case LIST_BENEFICIAIRES:
					processBeneficiaires(xmlStreamReader, callback);
					break;
				default:
					xmlStreamReader.next();
				}
			}
			else {
				xmlStreamReader.next();
			}

		}
		callback.done();
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
	private void processServitudes(XMLStreamReader xmlStreamReader, Callback callback) throws JAXBException, XMLStreamException {

		final Unmarshaller unmarshaller = xmlHelperRF.getServitudeContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (SERVITUDE.equals(localName)) {
					final Dienstbarkeit servitude = (Dienstbarkeit) unmarshaller.unmarshal(xmlStreamReader);
					if (servitude != null) {
						callback.onServitude(servitude);
					}
				}
				else {
					// on ignore les autres servitudes de type Anmerkung, Vormerkung et Grundlast
					xmlStreamReader.next();
				}
			}
			else if (eventType == XMLStreamConstants.END_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (LIST_SERVITUDES.equals(localName)) {
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

	/**
	 * Exemple XML :
	 * <pre>
	 *    &lt;recht:LastRechtGruppeList&gt;
	 *        &lt;recht:LastRechtGruppe VersionID="1f109152380ffd8901380fff10cc634f"&gt;
	 *            &lt;recht:StandardRechtIDREF&gt;_1f109152380ffd8901380fff10ca631e&lt;/recht:StandardRechtIDREF&gt;
	 *            &lt;recht:BelastetesGrundstueck VersionID="1f109152380ffd8901380fff10d363d5"&gt;
	 *                &lt;recht:BelastetesGrundstueckIDREF&gt;_1f109152380ffd8901380ffe15bd72c0&lt;/recht:BelastetesGrundstueckIDREF&gt;
	 *            &lt;/recht:BelastetesGrundstueck&gt;
	 *            &lt;recht:BerechtigtePerson VersionID="1f109152380ffd8901380fff10ce639e"&gt;
	 *                &lt;recht:NatuerlichePersonGb VersionID="1f109152380ffd8901380ffe267023c0" MasterID="1f109152380ffd8901380ffe266c238e"&gt;
	 *                    &lt;recht:Name&gt;Meylan&lt;/recht:Name&gt;
	 *                    &lt;recht:Status&gt;definitiv&lt;/recht:Status&gt;
	 *                    &lt;recht:Vorname&gt;Pierre&lt;/recht:Vorname&gt;
	 *                    &lt;recht:Geburtsdatum&gt;
	 *                        &lt;recht:Tag&gt;4&lt;/recht:Tag&gt;
	 *                        &lt;recht:Monat&gt;11&lt;/recht:Monat&gt;
	 *                        &lt;recht:Jahr&gt;1934&lt;/recht:Jahr&gt;
	 *                    &lt;/recht:Geburtsdatum&gt;
	 *                    &lt;recht:Zivilstand&gt;unbekannt&lt;/recht:Zivilstand&gt;
	 *                    &lt;recht:NameEltern&gt;Constant&lt;/recht:NameEltern&gt;
	 *                    &lt;recht:WeitereVornamen&gt;Eugène&lt;/recht:WeitereVornamen&gt;
	 *                    &lt;recht:PersonstammIDREF&gt;_1f109152381091220138109237ca2768&lt;/recht:PersonstammIDREF&gt;
	 *                &lt;/recht:NatuerlichePersonGb&gt;
	 *            &lt;/recht:BerechtigtePerson&gt;
	 *        &lt;/recht:LastRechtGruppe&gt;
	 *        &lt;recht:LastRechtGruppe VersionID="1f109152380ffd8901380fff1f496d54"&gt;
	 *            &lt;recht:StandardRechtIDREF&gt;_1f109152380ffd8901380fff1f476d0d&lt;/recht:StandardRechtIDREF&gt;
	 *            &lt;recht:BelastetesGrundstueck VersionID="1f109152380ffd8901380fff1f4e6dbc"&gt;
	 *                &lt;recht:BelastetesGrundstueckIDREF&gt;_1f109152380ffd8901380ffe025f139d&lt;/recht:BelastetesGrundstueckIDREF&gt;
	 *            &lt;/recht:BelastetesGrundstueck&gt;
	 *            &lt;recht:BerechtigtePerson VersionID="1f109152380ffd8901380fff1f4c6d95"&gt;
	 *                &lt;recht:NatuerlichePersonGb VersionID="1f109152380ffd8901380ffe26e925fb" MasterID="1f109152380ffd8901380ffe26e825c9"&gt;
	 *                    &lt;recht:Name&gt;Bonard&lt;/recht:Name&gt;
	 *                    &lt;recht:Status&gt;definitiv&lt;/recht:Status&gt;
	 *                    &lt;recht:Vorname&gt;Michèle&lt;/recht:Vorname&gt;
	 *                    &lt;recht:Ledigname&gt;Bosson&lt;/recht:Ledigname&gt;
	 *                    &lt;recht:Geburtsdatum&gt;
	 *                        &lt;recht:Tag&gt;7&lt;/recht:Tag&gt;
	 *                        &lt;recht:Monat&gt;12&lt;/recht:Monat&gt;
	 *                        &lt;recht:Jahr&gt;1940&lt;/recht:Jahr&gt;
	 *                    &lt;/recht:Geburtsdatum&gt;
	 *                    &lt;recht:Zivilstand&gt;unbekannt&lt;/recht:Zivilstand&gt;
	 *                    &lt;recht:NameEltern&gt;Robert&lt;/recht:NameEltern&gt;
	 *                    &lt;recht:NameEhegatte&gt;André&lt;/recht:NameEhegatte&gt;
	 *                    &lt;recht:WeitereVornamen&gt;Hélène&lt;/recht:WeitereVornamen&gt;
	 *                    &lt;recht:PersonstammIDREF&gt;_1f109152381059670138105a72ae5581&lt;/recht:PersonstammIDREF&gt;
	 *                &lt;/recht:NatuerlichePersonGb&gt;
	 *            &lt;/recht:BerechtigtePerson&gt;
	 *        &lt;/recht:LastRechtGruppe&gt;
	 *    &lt;/recht:LastRechtGruppeList&gt;
	 * </pre>
	 */
	private void processGroupesBeneficiaires(XMLStreamReader xmlStreamReader, Callback callback) throws JAXBException, XMLStreamException {

		final Unmarshaller unmarshaller = xmlHelperRF.getGroupeBeneficiairesContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (GROUPE_BENEFICIAIRES.equals(localName)) {
					final LastRechtGruppe beneficiaires = (LastRechtGruppe) unmarshaller.unmarshal(xmlStreamReader);
					if (beneficiaires != null) {
						callback.onGroupeBeneficiaires(beneficiaires);
					}
				}
				else {
					// on ignore les autres servitudes de type Anmerkung, Vormerkung et Grundlast
					xmlStreamReader.next();
				}
			}
			else if (eventType == XMLStreamConstants.END_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (LIST_GROUPES_BENEFICIAIRES.equals(localName)) {
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

	/**
	 * Exemple XML:
	 * <pre>
	 * &lt;recht:PersonstammList&gt;
	 *     &lt;recht:NatuerlichePersonstamm&gt;
	 *         &lt;recht:PersonstammID&gt;_1f109152380ffd8901380ffda2d01161&lt;/recht:PersonstammID&gt;
	 *         &lt;recht:Name&gt;Croisier&lt;/recht:Name&gt;
	 *         &lt;recht:Gueltig&gt;true&lt;/recht:Gueltig&gt;
	 *         &lt;recht:ClientRegulier&gt;false&lt;/recht:ClientRegulier&gt;
	 *         &lt;recht:NoSCC&gt;0&lt;/recht:NoSCC&gt;
	 *         &lt;recht:Status&gt;definitiv&lt;/recht:Status&gt;
	 *         &lt;recht:Sprache&gt;
	 *             &lt;recht:TextDe&gt;Französisch&lt;/recht:TextDe&gt;
	 *             &lt;recht:TextFr&gt;Français&lt;/recht:TextFr&gt;
	 *         &lt;/recht:Sprache&gt;
	 *         &lt;recht:Anrede&gt;
	 *             &lt;recht:TextDe&gt;*Madame&lt;/recht:TextDe&gt;
	 *             &lt;recht:TextFr&gt;Madame&lt;/recht:TextFr&gt;
	 *         &lt;/recht:Anrede&gt;
	 *         &lt;recht:NrACI&gt;0&lt;/recht:NrACI&gt;
	 *         &lt;recht:Adressen&gt;
	 *             &lt;recht:Strasse&gt; Rue Paul Golay 13&lt;/recht:Strasse&gt;
	 *             &lt;recht:PLZ&gt;1341&lt;/recht:PLZ&gt;
	 *             &lt;recht:Ort&gt;Orient&lt;/recht:Ort&gt;
	 *             &lt;recht:Rolle&gt;ACI&lt;/recht:Rolle&gt;
	 *         &lt;/recht:Adressen&gt;
	 *         &lt;recht:Vorname&gt;Josiane&lt;/recht:Vorname&gt;
	 *         &lt;recht:Zivilstand&gt;unbekannt&lt;/recht:Zivilstand&gt;
	 *         &lt;recht:Geburtsdatum&gt;
	 *             &lt;recht:Tag&gt;4&lt;/recht:Tag&gt;
	 *             &lt;recht:Monat&gt;12&lt;/recht:Monat&gt;
	 *             &lt;recht:Jahr&gt;1942&lt;/recht:Jahr&gt;
	 *         &lt;/recht:Geburtsdatum&gt;
	 *         &lt;recht:LedigName&gt;Croisier&lt;/recht:LedigName&gt;
	 *         &lt;recht:NameDerEltern&gt;Jean-Claude&lt;/recht:NameDerEltern&gt;
	 *         &lt;recht:Geschlecht&gt;unbekannt&lt;/recht:Geschlecht&gt;
	 *         &lt;recht:NameEhegatte&gt;Daniel&lt;/recht:NameEhegatte&gt;
	 *         &lt;recht:NrIROLE&gt;10384264&lt;/recht:NrIROLE&gt;
	 *     &lt;/recht:NatuerlichePersonstamm&gt;
	 *     &lt;recht:JuristischePersonstamm&gt;
	 *         &lt;recht:PersonstammID&gt;_1f109152380fff8e01380fffa2d404f7&lt;/recht:PersonstammID&gt;
	 *         &lt;recht:Name&gt;Ski-Club de Rougemont&lt;/recht:Name&gt;
	 *         &lt;recht:Gueltig&gt;true&lt;/recht:Gueltig&gt;
	 *         &lt;recht:ClientRegulier&gt;false&lt;/recht:ClientRegulier&gt;
	 *         &lt;recht:NoSCC&gt;0&lt;/recht:NoSCC&gt;
	 *         &lt;recht:Status&gt;definitiv&lt;/recht:Status&gt;
	 *         &lt;recht:Sprache&gt;
	 *             &lt;recht:TextDe&gt;Französisch&lt;/recht:TextDe&gt;
	 *             &lt;recht:TextFr&gt;Français&lt;/recht:TextFr&gt;
	 *         &lt;/recht:Sprache&gt;
	 *         &lt;recht:NrACI&gt;0&lt;/recht:NrACI&gt;
	 *         &lt;recht:Adressen&gt;
	 *             &lt;recht:PLZ&gt;1838&lt;/recht:PLZ&gt;
	 *             &lt;recht:Ort&gt;Rougemont&lt;/recht:Ort&gt;
	 *             &lt;recht:Rolle&gt;ACI&lt;/recht:Rolle&gt;
	 *         &lt;/recht:Adressen&gt;
	 *         &lt;recht:Sitz&gt;Rougemont&lt;/recht:Sitz&gt;
	 *         &lt;recht:Gesellschaftsform&gt;
	 *             &lt;recht:TextDe&gt;*Association&lt;/recht:TextDe&gt;
	 *             &lt;recht:TextFr&gt;Association&lt;/recht:TextFr&gt;
	 *         &lt;/recht:Gesellschaftsform&gt;
	 *         &lt;recht:Unterart&gt;SchweizerischeJuristischePerson&lt;/recht:Unterart&gt;
	 *     &lt;/recht:JuristischePersonstamm&gt;
	 * &lt;recht:PersonstammList&gt;
	 * </pre>
	 */
	private void processBeneficiaires(XMLStreamReader xmlStreamReader, Callback callback) throws JAXBException, XMLStreamException {

		final Unmarshaller unmarshaller = xmlHelperRF.getBeneficiaireContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (BENEFICIAIRE_PP.equals(localName) || BENEFICIAIRE_PM.equals(localName)) {
					final Personstamm beneficiaire = (Personstamm) unmarshaller.unmarshal(xmlStreamReader);
					if (beneficiaire != null) {
						callback.onBeneficiaire(beneficiaire);
					}
				}
				else {
					// on ignore les autres servitudes de type Anmerkung, Vormerkung et Grundlast
					xmlStreamReader.next();
				}
			}
			else if (eventType == XMLStreamConstants.END_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (LIST_BENEFICIAIRES.equals(localName)) {
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
