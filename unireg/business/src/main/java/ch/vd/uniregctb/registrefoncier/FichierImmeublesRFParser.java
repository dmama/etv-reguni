package ch.vd.uniregctb.registrefoncier;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.stax2.XMLInputFactory2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;

/**
 * Classe qui parse un fichier d'import d'immeubles du registre foncier et qui notifie au fil du parsing des éléments lus.
 */
public class FichierImmeublesRFParser {

	public static final String GRUNDSTUECK_NAMESPACE = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck";

	private static final String LIST_IMMEUBLES = "GrundstueckList";
	private static final String BIEN_FOND = "Liegenschaft";
	private static final String DDP = "Sdr";    // droit distinct et permanent
	private static final String PPE = "StockwerksEinheit";
	private static final String COOPERATIVE = "GewoehnlichesMiteigentum";
	private static final String FOLIO = "Folio";
	private static final String MINE = "Bergwerk";
	private static final String TERRAIN_INCONNU = "UnbekanntesGrundstueck";
	private static final Set<String> TYPES_IMMEUBLES = new HashSet<>(Arrays.asList(BIEN_FOND, DDP, PPE, COOPERATIVE, FOLIO, MINE, TERRAIN_INCONNU));

	private static final String LIST_DROITS = "EigentumList";
	private static final String DROIT_PROPRIETE_PERSONNEL = "PersonEigentumAnteil";


	private static final String LIST_PROPRIETAIRES = "PersonstammList";
	private static final String PROP_PERSONNE_PHYSIQUE = "NatuerlichePersonstamm";
	private static final String PROP_PERSONNE_MORALE = "JuristischePersonstamm";

	private static final String LIST_BATIMENTS = "GebaeudeList";
	private static final String BATIMENT = "Gebaeude";

	private static final String LIST_SURFACES = "BodenbedeckungList";
	private static final String SURFACE = "Bodenbedeckung";

	private XmlHelperRF xmlHelperRF;

	public void setXmlHelperRF(XmlHelperRF xmlHelperRF) {
		this.xmlHelperRF = xmlHelperRF;
	}

	/**
	 * Interface orientée-événement pour recevoir les entités au fur et à mesure qu'elles sont parsées.
	 * <p/>
	 * L'ordre de réception des données est le suivant :
	 * <ul>
	 *     <li>Immeubles</li>
	 *     <li>Droits</li>
	 *     <li>Propriétaires</li>
	 *     <li>Bâtiments</li>
	 *     <li>Surfaces au sol</li>
	 * </ul>
	 */
	public interface Callback {

		void onImmeuble(@NotNull Grundstueck immeuble);

		void onDroit(@NotNull PersonEigentumAnteil droit);

		void onProprietaire(@NotNull Personstamm personne);

		void onBatiment(@NotNull Gebaeude construction);

		void onSurface(@NotNull Bodenbedeckung surface);

		/**
		 * Méthode appelée lorsque toutes les données ont été envoyées.
		 */
		void done();
	}

	/**
	 * Parse le fichier spécifié. Les éléments extraits sont immédiatement proposé au processing à travers l'interface de callback.
	 *
	 * @param is            le flux du fichier à parser
	 * @param callback      une interface de callback pour recevoir en flux tendu les éléments parsés.
	 * @param statusManager un status manager pour suivre la progression du traitement
	 * @throws XMLStreamException exception levée si le fichier n'est pas valide XML
	 * @throws JAXBException      exception levée si JAXB n'arrive pas interpréter le fichier XML
	 */
	public void processFile(@NotNull InputStream is, @NotNull Callback callback, @Nullable StatusManager statusManager) throws XMLStreamException, JAXBException {

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
				if (!GRUNDSTUECK_NAMESPACE.equals(xmlStreamReader.getNamespaceURI())) {
					throw new IllegalArgumentException("Le namespace du fichier n'est pas le bon");
				}
				final String localName = xmlStreamReader.getLocalName();
				switch (localName) {
				case LIST_IMMEUBLES:
					if (statusManager != null) {
						statusManager.setMessage("Détection des mutations sur les immeubles...", 0);
					}
					processImmeubles(xmlStreamReader, callback);
					break;
				case LIST_DROITS:
					if (statusManager != null) {
						statusManager.setMessage("Détection des mutations sur les droits...", 20);
					}
					processDroits(xmlStreamReader, callback);
					break;
				case LIST_PROPRIETAIRES:
					if (statusManager != null) {
						statusManager.setMessage("Détection des mutations sur les propriétaires...", 40);
					}
					processProprietaires(xmlStreamReader, callback);
					break;
				case LIST_BATIMENTS:
					if (statusManager != null) {
						statusManager.setMessage("Détection des mutations sur les bâtiments...", 60);
					}
					processBatiments(xmlStreamReader, callback);
					break;
				case LIST_SURFACES:
					if (statusManager != null) {
						statusManager.setMessage("Détection des mutations sur les surfaces...", 80);
					}
					processSurfaces(xmlStreamReader, callback);
					break;
				default:
					xmlStreamReader.next();
				}
			}
			else {
				xmlStreamReader.next();
			}

		}
		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations terminé.", 100);
		}
		callback.done();
		xmlStreamReader.close();
	}

	/**
	 * Exemple de XML :
	 * <pre>
	 * 	&lt;GrundstueckList&gt;
	 * 		&lt;Liegenschaft&gt;
	 * 			&lt;GrundstueckID&gt;_1f109152381026b501381028a73d1852&lt;/GrundstueckID&gt;
	 * 			&lt;EGrid&gt;CH938391457759&lt;/EGrid&gt;
	 * 			&lt;GrundstueckNummer VersionID="1f109152381026b501381028a74018e1"&gt;
	 * 			&lt;BfsNr&gt;294&lt;/BfsNr&gt;
	 * 			&lt;Gemeindenamen&gt;Oron&lt;/Gemeindenamen&gt;
	 * 			&lt;StammNr&gt;5089&lt;/StammNr&gt;
	 * 			&lt;/GrundstueckNummer&gt;
	 * 		    ...
	 * 		&lt;/Liegenschaft&gt;
	 * 		&lt;Sdr&gt;
	 * 			&lt;GrundstueckID&gt;_8af806cc3971feb60139e36d062130f3&lt;/GrundstueckID&gt;
	 * 			&lt;EGrid&gt;CH729253834531&lt;/EGrid&gt;
	 * 			&lt;GrundstueckNummer VersionID="1f109152381026b501381028ab3f31b8"&gt;
	 * 			&lt;BfsNr&gt;294&lt;/BfsNr&gt;
	 * 			&lt;Gemeindenamen&gt;Oron&lt;/Gemeindenamen&gt;
	 * 			&lt;StammNr&gt;692&lt;/StammNr&gt;
	 * 			&lt;/GrundstueckNummer&gt;
	 * 		    ...
	 * 		&lt;/Sdr&gt;
	 * 		&lt;StockwerksEinheit&gt;
	 * 			&lt;GrundstueckID&gt;_8af806fc45d223e60149c23f475365d5&lt;/GrundstueckID&gt;
	 * 			&lt;EGrid&gt;CH336583651349&lt;/EGrid&gt;
	 * 			&lt;GrundstueckNummer VersionID="8af806fc45d223e60149139250510365"&gt;
	 * 			&lt;BfsNr&gt;190&lt;/BfsNr&gt;
	 * 			&lt;Gemeindenamen&gt;Boulens&lt;/Gemeindenamen&gt;
	 * 			&lt;StammNr&gt;19&lt;/StammNr&gt;
	 * 			&lt;IndexNr1&gt;4&lt;/IndexNr1&gt;
	 * 			&lt;/GrundstueckNummer&gt;
	 * 		    ...
	 * 		&lt;/StockwerksEinheit&gt;
	 * 		&lt;GewoehnlichesMiteigentum&gt;
	 * 			&lt;GrundstueckID&gt;_8af806cc5043853201508e1e8a3a1a71&lt;/GrundstueckID&gt;
	 * 			&lt;EGrid&gt;CH516579658411&lt;/EGrid&gt;
	 * 			&lt;GrundstueckNummer VersionID="8af806fc4e7cb94d014ffe532ff45108"&gt;
	 * 			&lt;BfsNr&gt;308&lt;/BfsNr&gt;
	 * 			&lt;Gemeindenamen&gt;Corcelles-près-Payerne&lt;/Gemeindenamen&gt;
	 * 			&lt;StammNr&gt;3601&lt;/StammNr&gt;
	 * 			&lt;IndexNr1&gt;7&lt;/IndexNr1&gt;
	 * 			&lt;IndexNr2&gt;13&lt;/IndexNr2&gt;
	 * 			&lt;/GrundstueckNummer&gt;
	 * 		    ...
	 * 		&lt;/GewoehnlichesMiteigentum&gt;
	 * 	&lt;/GrundstueckList&gt;
	 * </pre>
	 */
	private void processImmeubles(@NotNull XMLStreamReader xmlStreamReader, @NotNull Callback callback) throws XMLStreamException, JAXBException {

		final Unmarshaller unmarshaller = xmlHelperRF.getImmeubleContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (TYPES_IMMEUBLES.contains(localName)) {
					final Grundstueck immeuble = (Grundstueck) unmarshaller.unmarshal(xmlStreamReader);
					if (immeuble != null) {
						callback.onImmeuble(immeuble);
					}
				}
				else {
					xmlStreamReader.next();
				}
			}
			else if (eventType == XMLStreamConstants.END_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (LIST_IMMEUBLES.equals(localName)) {
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
	 * Exemple de XML:
	 * <pre>
	 * 	&lt;EigentumList&gt;
	 * 		&lt;PersonEigentumAnteil VersionID="1f109152380ffd8901380ffeb9894d82" MasterID="1f109152380ffd8901380ffe560b63c0"&gt;
	 * 			&lt;Quote&gt;
	 * 				&lt;AnteilZaehler&gt;1&lt;/AnteilZaehler&gt;
	 * 				&lt;AnteilNenner&gt;2&lt;/AnteilNenner&gt;
	 * 				&lt;QuoteUnbekannt&gt;false&lt;/QuoteUnbekannt&gt;
	 * 			&lt;/Quote&gt;
	 * 			&lt;BelastetesGrundstueckIDREF&gt;_1f109152380ffd8901380ffe08d626e9&lt;/BelastetesGrundstueckIDREF&gt;
	 * 			&lt;NatuerlichePersonGb VersionID="1f109152380ffd8901380ffe561063f2" MasterID="1f109152380ffd8901380ffeb9894d83"&gt;
	 * 				&lt;Name&gt;Gay&lt;/Name&gt;
	 * 				&lt;Status&gt;definitiv&lt;/Status&gt;
	 * 			    ...
	 * 			&lt;/NatuerlichePersonGb&gt;
	 * 			&lt;PersonEigentumsForm&gt;miteigentum&lt;/PersonEigentumsForm&gt;
	 * 			&lt;AnzahlPaquiers&gt;0&lt;/AnzahlPaquiers&gt;
	 * 		&lt;/PersonEigentumAnteil&gt;
	 * 		&lt;PersonEigentumAnteil VersionID="1f1091523810039001381006a0e56990" MasterID="1f10915238100390013810060a314f95"&gt;
	 * 			&lt;Quote&gt;
	 * 				&lt;AnteilZaehler&gt;1&lt;/AnteilZaehler&gt;
	 * 				&lt;AnteilNenner&gt;6&lt;/AnteilNenner&gt;
	 * 				&lt;QuoteUnbekannt&gt;false&lt;/QuoteUnbekannt&gt;
	 * 			&lt;/Quote&gt;
	 * 			&lt;BelastetesGrundstueckIDREF&gt;_1f1091523810039001381005406907e5&lt;/BelastetesGrundstueckIDREF&gt;
	 * 			&lt;Gemeinschaft VersionID="1f10915238100390013810060a344fc7" MasterID="1f1091523810039001381006a0e56995"&gt;
	 * 				&lt;Status&gt;definitiv&lt;/Status&gt;
	 * 				&lt;GemeinschatID&gt;_1f10915238100390013810060a314f95&lt;/GemeinschatID&gt;
	 * 				&lt;Art&gt;Erbengemeinschaft&lt;/Art&gt;
	 * 			&lt;/Gemeinschaft&gt;
	 * 			&lt;PersonEigentumsForm&gt;miteigentum&lt;/PersonEigentumsForm&gt;
	 * 			&lt;AnzahlPaquiers&gt;0&lt;/AnzahlPaquiers&gt;
	 * 		&lt;/PersonEigentumAnteil&gt;
	 * 		&lt;PersonEigentumAnteil VersionID="1f109152381009be0138100e1b0e3546" MasterID="1f109152381009be0138100c91a023e9"&gt;
	 * 			&lt;Quote&gt;
	 * 				&lt;AnteilZaehler&gt;1&lt;/AnteilZaehler&gt;
	 * 				&lt;AnteilNenner&gt;1&lt;/AnteilNenner&gt;
	 * 				&lt;QuoteUnbekannt&gt;false&lt;/QuoteUnbekannt&gt;
	 * 			&lt;/Quote&gt;
	 * 			&lt;BelastetesGrundstueckIDREF&gt;_1f109152381009be0138100bd8d01389&lt;/BelastetesGrundstueckIDREF&gt;
	 * 			&lt;JuristischePersonGb VersionID="1f109152381009be0138100c91a2242d" MasterID="1f109152381009be0138100e1b0e3547"&gt;
	 * 				&lt;Name&gt;Confédération suisse&lt;/Name&gt;
	 * 				&lt;Status&gt;definitiv&lt;/Status&gt;
	 * 				&lt;Sitz&gt;Berne&lt;/Sitz&gt;
	 * 				&lt;PersonstammIDREF&gt;_1f10915238109122013810913723057f&lt;/PersonstammIDREF&gt;
	 * 			&lt;/JuristischePersonGb&gt;
	 * 			&lt;PersonEigentumsForm&gt;alleineigentum&lt;/PersonEigentumsForm&gt;
	 * 			&lt;AnzahlPaquiers&gt;0&lt;/AnzahlPaquiers&gt;
	 * 		&lt;/PersonEigentumAnteil&gt;
	 * 		&lt;GrundstueckEigentumAnteil VersionID="8af806fa40347c3301409c00a0cc25e1" MasterID="8af806fa40347c3301409c00a0cd25e2"&gt;
	 * 			&lt;Quote&gt;
	 * 				&lt;AnteilZaehler&gt;1&lt;/AnteilZaehler&gt;
	 * 				&lt;AnteilNenner&gt;14&lt;/AnteilNenner&gt;
	 * 				&lt;QuoteUnbekannt&gt;false&lt;/QuoteUnbekannt&gt;
	 * 			&lt;/Quote&gt;
	 * 			&lt;BelastetesGrundstueckIDREF&gt;_8af806fa40347c3301409bf788af25a5&lt;/BelastetesGrundstueckIDREF&gt;
	 * 			&lt;BerechtigtesGrundstueckIDREF&gt;_8af806fa40347c3301409c009e4e25db&lt;/BerechtigtesGrundstueckIDREF&gt;
	 * 			&lt;GrundstueckEigentumsForm&gt;Miteigentum&lt;/GrundstueckEigentumsForm&gt;
	 * 			&lt;Rechtsgruende&gt;
	 * 				&lt;AmtNummer&gt;6&lt;/AmtNummer&gt;
	 * 				&lt;RechtsgrundCode&gt;
	 * 					&lt;TextDe&gt;*Constitution de parts de copropriété&lt;/TextDe&gt;
	 * 					&lt;TextFr&gt;Constitution de parts de copropriété&lt;/TextFr&gt;
	 * 				&lt;/RechtsgrundCode&gt;
	 * 		        ...
	 * 			&lt;/Rechtsgruende&gt;
	 * 			&lt;IndexBS&gt;0&lt;/IndexBS&gt;
	 * 		&lt;/GrundstueckEigentumAnteil&gt;
	 * 	&lt;/EigentumList&gt;
	 * </pre>
	 */
	private void processDroits(@NotNull XMLStreamReader xmlStreamReader, @NotNull Callback callback) throws XMLStreamException, JAXBException {

		final Unmarshaller unmarshaller = xmlHelperRF.getDroitContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (DROIT_PROPRIETE_PERSONNEL.equals(localName)) {
					final PersonEigentumAnteil droit = (PersonEigentumAnteil) unmarshaller.unmarshal(xmlStreamReader);
					if (droit != null) {
						callback.onDroit(droit);
					}
				}
				else {
					// on ignore les autres droits de type GrundstueckEigentumAnteil et HerrenlosEigentum
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

	/**
	 * Exemple de XML :
	 * <pre>
	 * 	&lt;PersonstammList&gt;
	 * 		&lt;NatuerlichePersonstamm&gt;
	 * 			&lt;PersonstammID&gt;_1f109152380ffd8901380ffda31b11e2&lt;/PersonstammID&gt;
	 * 			&lt;Name&gt;Debonneville&lt;/Name&gt;
	 * 			&lt;Gueltig&gt;true&lt;/Gueltig&gt;
	 * 			&lt;ClientRegulier&gt;false&lt;/ClientRegulier&gt;
	 * 			&lt;NoSCC&gt;0&lt;/NoSCC&gt;
	 * 			&lt;Status&gt;definitiv&lt;/Status&gt;
	 * 			&lt;Sprache&gt;
	 * 				&lt;TextDe&gt;Französisch&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Français&lt;/TextFr&gt;
	 * 			&lt;/Sprache&gt;
	 * 			&lt;Anrede&gt;
	 * 				&lt;TextDe&gt;*Monsieur&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Monsieur&lt;/TextFr&gt;
	 * 			&lt;/Anrede&gt;
	 * 			&lt;NoRF&gt;209544&lt;/NoRF&gt;
	 * 			&lt;Vorname&gt;Jean-Luc&lt;/Vorname&gt;
	 * 			&lt;Zivilstand&gt;unbekannt&lt;/Zivilstand&gt;
	 * 			&lt;Geburtsdatum&gt;
	 * 				&lt;Tag&gt;21&lt;/Tag&gt;
	 * 				&lt;Monat&gt;5&lt;/Monat&gt;
	 * 				&lt;Jahr&gt;1961&lt;/Jahr&gt;
	 * 			&lt;/Geburtsdatum&gt;
	 * 			&lt;NameDerEltern&gt;Jacques&lt;/NameDerEltern&gt;
	 * 			&lt;Geschlecht&gt;unbekannt&lt;/Geschlecht&gt;
	 * 		&lt;/NatuerlichePersonstamm&gt;
	 * 		&lt;JuristischePersonstamm&gt;
	 * 			&lt;PersonstammID&gt;_1f1091523810039001381003a44d07a1&lt;/PersonstammID&gt;
	 * 			&lt;Name&gt;Société du pâturage des Seytorées&lt;/Name&gt;
	 * 			&lt;Gueltig&gt;true&lt;/Gueltig&gt;
	 * 			&lt;ClientRegulier&gt;false&lt;/ClientRegulier&gt;
	 * 			&lt;NoSCC&gt;0&lt;/NoSCC&gt;
	 * 			&lt;Status&gt;definitiv&lt;/Status&gt;
	 * 			&lt;Sprache&gt;
	 * 				&lt;TextDe&gt;Französisch&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Français&lt;/TextFr&gt;
	 * 			&lt;/Sprache&gt;
	 * 			&lt;NoRF&gt;113800&lt;/NoRF&gt;
	 * 			&lt;Sitz&gt;Payerne&lt;/Sitz&gt;
	 * 			&lt;Gesellschaftsform&gt;
	 * 				&lt;TextDe&gt;*Société coopérative&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Société coopérative&lt;/TextFr&gt;
	 * 			&lt;/Gesellschaftsform&gt;
	 * 			&lt;Unterart&gt;SchweizerischeJuristischePerson&lt;/Unterart&gt;
	 * 		&lt;/JuristischePersonstamm&gt;
	 * 	&lt;/PersonstammList&gt;
	 * </pre>
	 */
	private void processProprietaires(@NotNull XMLStreamReader xmlStreamReader, @NotNull Callback callback) throws XMLStreamException, JAXBException {

		final Unmarshaller unmarshaller = xmlHelperRF.getProprietaireContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (PROP_PERSONNE_PHYSIQUE.equals(localName) || PROP_PERSONNE_MORALE.equals(localName)) {
					final Personstamm proprietaire = (Personstamm) unmarshaller.unmarshal(xmlStreamReader);
					if (proprietaire != null) {
						callback.onProprietaire(proprietaire);
					}
				}
				else {
					xmlStreamReader.next();
				}
			}
			else if (eventType == XMLStreamConstants.END_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (LIST_PROPRIETAIRES.equals(localName)) {
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
	 * Exemple de XML:
	 * <pre>
	 * 	&lt;GebaeudeList&gt;
	 * 		&lt;Gebaeude VersionID="1f109152380ffd8901380ffe81c91455" MasterID="1f109152380ffd8901380ffe81c4141d"&gt;
	 * 			&lt;GrundstueckZuGebaeude&gt;
	 * 				&lt;GrundstueckIDREF&gt;_1f109152380ffd8901380ffe19367d20&lt;/GrundstueckIDREF&gt;
	 * 				&lt;AbschnittFlaeche&gt;157&lt;/AbschnittFlaeche&gt;
	 * 			&lt;/GrundstueckZuGebaeude&gt;
	 * 			&lt;Einzelobjekt&gt;false&lt;/Einzelobjekt&gt;
	 * 			&lt;Unterirdisch&gt;false&lt;/Unterirdisch&gt;
	 * 			&lt;MehrereGrundstuecke&gt;false&lt;/MehrereGrundstuecke&gt;
	 * 			&lt;GebaeudeArten&gt;
	 * 				&lt;GebaeudeArtCode&gt;
	 * 					&lt;TextDe&gt;*Habitation avec affectation mixte&lt;/TextDe&gt;
	 * 					&lt;TextFr&gt;Habitation avec affectation mixte&lt;/TextFr&gt;
	 * 				&lt;/GebaeudeArtCode&gt;
	 * 			&lt;/GebaeudeArten&gt;
	 * 			&lt;Versicherungsnummer&gt;142&lt;/Versicherungsnummer&gt;
	 * 		&lt;/Gebaeude&gt;
	 * 		&lt;Gebaeude VersionID="8af8064d567f817b0156eb1d2f891cee" MasterID="1f109152380ffd8901380ffe894f41ae"&gt;
	 * 			&lt;GrundstueckZuGebaeude&gt;
	 * 				&lt;GrundstueckIDREF&gt;_1f109152380ffd8901380ffe07fb2421&lt;/GrundstueckIDREF&gt;
	 * 				&lt;AbschnittFlaeche&gt;107&lt;/AbschnittFlaeche&gt;
	 * 			&lt;/GrundstueckZuGebaeude&gt;
	 * 			&lt;Einzelobjekt&gt;false&lt;/Einzelobjekt&gt;
	 * 			&lt;Unterirdisch&gt;false&lt;/Unterirdisch&gt;
	 * 			&lt;MehrereGrundstuecke&gt;false&lt;/MehrereGrundstuecke&gt;
	 * 			&lt;Flaeche&gt;107&lt;/Flaeche&gt;
	 * 			&lt;GebaeudeArten&gt;
	 * 				&lt;GebaeudeArtCode&gt;
	 * 					&lt;TextDe&gt;*Habitation&lt;/TextDe&gt;
	 * 					&lt;TextFr&gt;Habitation&lt;/TextFr&gt;
	 * 				&lt;/GebaeudeArtCode&gt;
	 * 			&lt;/GebaeudeArten&gt;
	 * 			&lt;Versicherungsnummer&gt;1563a&lt;/Versicherungsnummer&gt;
	 * 		&lt;/Gebaeude&gt;
	 * 	&lt;/GebaeudeList&gt;
	 * </pre>
	 */
	private void processBatiments(@NotNull XMLStreamReader xmlStreamReader, @NotNull Callback callback) throws XMLStreamException, JAXBException {

		final Unmarshaller unmarshaller = xmlHelperRF.getBatimentContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (BATIMENT.equals(localName)) {
					final Gebaeude batiment = (Gebaeude) unmarshaller.unmarshal(xmlStreamReader);
					if (batiment != null) {
						callback.onBatiment(batiment);
					}
				}
				else {
					xmlStreamReader.next();
				}
			}
			else if (eventType == XMLStreamConstants.END_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (LIST_BATIMENTS.equals(localName)) {
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
	 * Exemple de XML:
	 * <pre>
	 * 	&lt;BodenbedeckungList&gt;
	 * 		&lt;Bodenbedeckung VersionID="1f109152381091220138109a8c19566e"&gt;
	 * 			&lt;GrundstueckIDREF&gt;_1f10915238109122013810973b5b5d7e&lt;/GrundstueckIDREF&gt;
	 * 			&lt;Art&gt;
	 * 				&lt;TextDe&gt;*Place-jardin&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Place-jardin&lt;/TextFr&gt;
	 * 			&lt;/Art&gt;
	 * 			&lt;Flaeche&gt;1766&lt;/Flaeche&gt;
	 * 		&lt;/Bodenbedeckung&gt;
	 * 		&lt;Bodenbedeckung VersionID="1f10915238102ecd0138103217692dbd"&gt;
	 * 			&lt;GrundstueckIDREF&gt;_1f10915238102ecd01381030e605091e&lt;/GrundstueckIDREF&gt;
	 * 			&lt;Art&gt;
	 * 				&lt;TextDe&gt;*Pré-champ&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Pré-champ&lt;/TextFr&gt;
	 * 			&lt;/Art&gt;
	 * 			&lt;Flaeche&gt;3457&lt;/Flaeche&gt;
	 * 		&lt;/Bodenbedeckung&gt;
	 * 		&lt;Bodenbedeckung VersionID="1f10915238107c150138108467353b41"&gt;
	 * 			&lt;GrundstueckIDREF&gt;_1f10915238107c1501381081420d5645&lt;/GrundstueckIDREF&gt;
	 * 			&lt;Art&gt;
	 * 				&lt;TextDe&gt;*Forêt&lt;/TextDe&gt;
	 * 				&lt;TextFr&gt;Forêt&lt;/TextFr&gt;
	 * 			&lt;/Art&gt;
	 * 			&lt;Flaeche&gt;3510&lt;/Flaeche&gt;
	 * 		&lt;/Bodenbedeckung&gt;
	 * 	&lt;/BodenbedeckungList&gt;
	 * </pre>
	 */
	private void processSurfaces(@NotNull XMLStreamReader xmlStreamReader, @NotNull Callback callback) throws XMLStreamException, JAXBException {

		final Unmarshaller unmarshaller = xmlHelperRF.getSurfacesAuSolContext().createUnmarshaller();

		while (xmlStreamReader.hasNext()) {
			final int eventType = xmlStreamReader.getEventType();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (SURFACE.equals(localName)) {
					final Bodenbedeckung surface = (Bodenbedeckung) unmarshaller.unmarshal(xmlStreamReader);
					if (surface != null) {
						callback.onSurface(surface);
					}
				}
				else {
					xmlStreamReader.next();
				}
			}
			else if (eventType == XMLStreamConstants.END_ELEMENT) {
				final String localName = xmlStreamReader.getLocalName();
				if (LIST_SURFACES.equals(localName)) {
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
