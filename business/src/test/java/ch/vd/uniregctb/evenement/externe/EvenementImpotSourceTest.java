package ch.vd.uniregctb.evenement.externe;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.math.BigInteger;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.util.StringSource;
import ch.vd.unireg.xml.event.lr.quittance.v1.EvtQuittanceListe;
import ch.vd.unireg.xml.event.lr.quittance.v1.Liste;
import ch.vd.unireg.xml.event.lr.quittance.v1.ObjectFactory;
import ch.vd.unireg.xml.event.lr.quittance.v1.Origine;
import ch.vd.unireg.xml.event.lr.quittance.v1.Quittance;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.common.XmlUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EvenementImpotSourceTest extends WithoutSpringTest {

	private static final Long NUMERO_CONTRIBUABLE = 1300002L;
	private Schema schemaCache;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());
		final ClassPathResource resource = new ClassPathResource("event/lr/evtQuittanceListe-v1.xsd");
		Source source = new StreamSource(resource.getURL().toExternalForm());
		schemaCache = sf.newSchema(source);
	}

	/**
	 * Ce test vérifie que la méthode createQuittance crée bien une quittance valide. Bon, ça fait pas de mal, mais ça ne sert pas non plus à grand-chose puisque la méthode createQuittance est propre au
	 * test.
	 */
	@Test
	public void serialize() throws Exception {
		final EvtQuittanceListe evenement = new EvtQuittanceListe();
		createQuittance(evenement, NUMERO_CONTRIBUABLE);
		assertTrue(validate(evenement));
	}

	/**
	 * Ce test vérifie qu'une quittance vide ne valide pas. Bon, encore une fois ça ne fait pas de mal, mais à quoi ça sert sachant qu'on n'envoie jamais des quittances (on les reçoit seulement) ?
	 */
	@Test
	public void serializeError() throws Exception {
		final EvtQuittanceListe evenement = new EvtQuittanceListe();
		assertFalse(validate(evenement));
	}

	/**
	 * Ce test vérifie que la quittance au format texte ci-dessous est bien parsée par la méthode 'parse' et que le résultat est valide. Ca ne fait pas de mal, mais ça n'apporte pas grand-chose non
	 * plus...
	 */
	@Test
	public void deserialize() throws Exception {
		final String xml =
				"<evtQuittanceListe xmlns=\"http://www.vd.ch/fiscalite/taxation/evtQuittanceListe-v1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.vd.ch/fiscalite/taxation/evtQuittanceListe-v1:\\repositories\\SVN_ACI\\taotra\\050_Conception\\technique\\xsd\\quittance\\evtQuittanceListe-v1.xsd\">\n" +
						"\t<timestampEvtQuittance>2001-12-17T09:30:47.0Z</timestampEvtQuittance>\n" +
						"\t<typeEvtQuittance>QUITTANCEMENT</typeEvtQuittance>\n" +
						"\t<origineListe>ELECTRONIQUE</origineListe>\n" +
						"\t<identificationListe>\n" +
						"\t\t<typeListe>LR</typeListe>\n" +
						"\t\t<numeroSequence>1</numeroSequence>\n" +
						"\t\t<numeroDebiteur>1300002</numeroDebiteur>\n" +
						"\t\t<periodeDeclaration>\n" +
						"\t\t\t<dateDebut>1967-08-13</dateDebut>\n" +
						"\t\t\t<dateFin>1967-08-13</dateFin>\n" +
						"\t\t</periodeDeclaration>\n" +
						"\t</identificationListe>\n" +
						"</evtQuittanceListe>";
		final EvtQuittanceListe document = parse(xml);
		assertTrue(validate(document));
	}

	private EvtQuittanceListe parse(String xml) throws JAXBException {

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		u.setSchema(schemaCache);

		final Source source = new StringSource(xml);
		final Object event = u.unmarshal(source);
		if (event == null) {
			return null;
		}

		// Crée l'événement correspondant
		if (event instanceof EvtQuittanceListe) {
			return (EvtQuittanceListe) event;
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + event.getClass());
		}
	}

	private void createQuittance(EvtQuittanceListe evenement, Long numeroDebiteur) {
		final EvtQuittanceListe.IdentificationListe identification = new EvtQuittanceListe.IdentificationListe();
		identification.setNumeroDebiteur(numeroDebiteur.intValue());
		final EvtQuittanceListe.IdentificationListe.PeriodeDeclaration periodeDeclaration = new EvtQuittanceListe.IdentificationListe.PeriodeDeclaration();
		periodeDeclaration.setDateDebut(XmlUtils.regdate2xmlcal(RegDate.get()));
		periodeDeclaration.setDateFin(XmlUtils.regdate2xmlcal(RegDate.get()));
		identification.setPeriodeDeclaration(periodeDeclaration);
		identification.setTypeListe(Liste.LR);
		identification.setNumeroSequence(new BigInteger("1"));
		evenement.setIdentificationListe(identification);
		evenement.setTypeEvtQuittance(Quittance.QUITTANCEMENT);
		evenement.setTimestampEvtQuittance(XmlUtils.regdate2xmlcal(RegDate.get()));
		evenement.setOrigineListe(Origine.ELECTRONIQUE);
	}

	private boolean validate(EvtQuittanceListe event) throws JAXBException, ParserConfigurationException {

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Marshaller marshaller = context.createMarshaller();
		marshaller.setSchema(schemaCache);

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();

		final QName qname = new QName("http://www.vd.ch/fiscalite/taxation/evtQuittanceListe-v1", "evtQuittanceListe");
		try {
			marshaller.marshal(new JAXBElement<>(qname, EvtQuittanceListe.class, null, event), doc);
			return true;
		}
		catch (MarshalException e) {
			return false;
		}
	}
}
