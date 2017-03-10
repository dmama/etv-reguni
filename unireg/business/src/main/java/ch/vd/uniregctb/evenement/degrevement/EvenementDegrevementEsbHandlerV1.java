package ch.vd.uniregctb.evenement.degrevement;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.event.degrevement.v1.Message;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.jms.EsbMessageHelper;

public class EvenementDegrevementEsbHandlerV1 implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDegrevementEsbHandlerV1.class);

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private HibernateTemplate hibernateTemplate;
	private EvenementDegrevementHandlerV1 handler;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setHandler(EvenementDegrevementHandlerV1 handler) {
		this.handler = handler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ch.vd.unireg.xml.event.degrevement.v1.ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		LOGGER.info("Arrivée d'un message de retour de formulaire de dégrèvement ICI (" + message.getBusinessId() + ")");

		AuthenticationHelper.pushPrincipal("JMS-RetourDegrevement");
		try {
			onMessage(message);
			hibernateTemplate.flush();      // flush de la session avant la fin de la zone "avec principal"
		}
		catch (EsbBusinessException e) {
			hibernateTemplate.flush();      // flush de la session avant la fin de la zone "avec principal"
			throw e;
		}
		catch (Exception e) {
			// les exceptions qui restent sont des erreurs transientes ou des bugs, normalement
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage message) throws EsbBusinessException {
		try {
			final Message retour = parse(message.getBodyAsSource());
			handler.onRetourDegrevement(retour, EsbMessageHelper.extractCustomHeaders(message));
		}
		catch (JAXBException | SAXException | IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
	}

	private Message parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return (Message) u.unmarshal(message);
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
			sf.setResourceResolver(new ClasspathCatalogResolver());
			final Source source = new StreamSource(new ClassPathResource("/event/degrevement/documentDematDegrevement-1.xsd").getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}
}
