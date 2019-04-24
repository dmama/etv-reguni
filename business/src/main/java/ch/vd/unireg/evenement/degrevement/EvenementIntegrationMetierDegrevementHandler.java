package ch.vd.unireg.evenement.degrevement;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.dperm.EvenementIntegrationMetierHandler;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.xml.degrevement.quittance.v1.QuittanceIntegrationMetierImmDetails;
import ch.vd.unireg.xml.event.degrevement.v1.Message;

/**
 * Handler spécifique pour les événements d'intégration métier du DPerm concernant les données de dégrèvement ICI
 */
public class EvenementIntegrationMetierDegrevementHandler implements EvenementIntegrationMetierHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementIntegrationMetierDegrevementHandler.class);

	private Schema schemaCache;
	private JAXBContext requestJaxbContext;
	private JAXBContext responseJaxbContext;

	private HibernateTemplate hibernateTemplate;
	private EvenementDegrevementHandler handler;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setHandler(EvenementDegrevementHandler handler) {
		this.handler = handler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.requestJaxbContext = JAXBContext.newInstance(ch.vd.unireg.xml.event.degrevement.v1.ObjectFactory.class.getPackage().getName());
		this.responseJaxbContext = JAXBContext.newInstance(ch.vd.unireg.xml.degrevement.quittance.v1.ObjectFactory.class.getPackage().getName());
	}

	@Override
	public Document handleMessage(Source xml, Map<String, String> metaDonnees) throws Exception {
		LOGGER.info("Arrivée d'un message de retour de formulaire de dégrèvement ICI");

		AuthenticationHelper.pushPrincipal("JMS-RetourDégrèvement");
		try {
			// lecture des données entrantes
			final Message message;
			try {
				message = parse(xml);
			}
			catch (JAXBException | SAXException | IOException e) {
				throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
			}

			// traitement et constitution de la réponse
			final QuittanceIntegrationMetierImmDetails quittance = handler.onRetourDegrevement(message, metaDonnees);

			// si on n'arrive pas à répondre (constitution et envoi du message de réponse, les données métiers étant
			// déjà contenues dans la quittance...) alors on ne peut qu'annuler la transaction...
			return buildDocument(quittance);
		}
		catch (EsbBusinessException e) {
			hibernateTemplate.flush();      // flush de la session avant la fin de la zone "avec principal"
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	protected Document buildDocument(QuittanceIntegrationMetierImmDetails quittance) throws JAXBException, ParserConfigurationException {
		final Marshaller marshaller = responseJaxbContext.createMarshaller();

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();
		marshaller.marshal(quittance, doc);
		return doc;
	}

	private Message parse(Source source) throws JAXBException, SAXException, IOException, EsbBusinessException {
		final Unmarshaller contenuUnmarshaller = requestJaxbContext.createUnmarshaller();
		contenuUnmarshaller.setSchema(getRequestSchema());
		return (Message) contenuUnmarshaller.unmarshal(source);
	}

	private Schema getRequestSchema() throws SAXException, IOException {
		if (schemaCache == null) {
			buildRequestSchema();
		}
		return schemaCache;
	}

	private synchronized void buildRequestSchema() throws SAXException, IOException {
		if (schemaCache == null) {
			final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			final Source source = new StreamSource(new ClassPathResource("/event/degrevement/documentDematDegrevement-1.xsd").getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}
}
