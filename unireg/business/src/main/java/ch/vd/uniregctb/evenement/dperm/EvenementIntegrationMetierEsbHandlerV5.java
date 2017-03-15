package ch.vd.uniregctb.evenement.dperm;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.dperm.xml.common.v1.TypeDocument;
import ch.vd.dperm.xml.integration.v5.ElementsIntegrationMetier;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.jms.EsbMessageHelper;
import ch.vd.uniregctb.jms.EsbMessageValidator;

public class EvenementIntegrationMetierEsbHandlerV5 implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementIntegrationMetierEsbHandlerV5.class);

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private HibernateTemplate hibernateTemplate;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private Map<TypeDocument, EvenementIntegrationMetierHandler> handlers;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setHandlers(Map<TypeDocument, EvenementIntegrationMetierHandler> handlers) {
		this.handlers = handlers;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbValidator(EsbMessageValidator esbValidator) {
		this.esbValidator = esbValidator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ch.vd.dperm.xml.integration.v5.ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		LOGGER.info("Arrivée d'un message ESB d'intégration métier DPerm (" + message.getBusinessId() + ")");

		AuthenticationHelper.pushPrincipal("JMS-IntégrationMétierDPerm");
		try {
			onMessage(message);
			hibernateTemplate.flush();      // pour conserver le principal autentifié
		}
		catch (EsbBusinessException e) {
			LOGGER.error(e.getMessage(), e);
			hibernateTemplate.flush();      // pour conserver le principal autentifié
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

	private void onMessage(EsbMessage message) throws Exception {
		final ElementsIntegrationMetier integration;
		try {
			integration = parse(message.getBodyAsSource());
		}
		catch (JAXBException | SAXException | IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}

		final EvenementIntegrationMetierHandler handler = handlers.get(integration.getTypeDocument());
		if (handler == null) {
			throw new EsbBusinessException(EsbBusinessCode.MESSAGE_NON_SUPPORTE, "Type de document non-supporté : " + integration.getTypeDocument(), null);
		}

		final String xmlInterne = StringEscapeUtils.unescapeXml(integration.getXmlUnitaire());
		final Document response = handler.handleMessage(xmlInterne, EsbMessageHelper.extractCustomHeaders(message));
		answer(message, response);
	}

	private void answer(EsbMessage request, Document responseBody) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage(request);
		m.setBusinessId(String.format("%s-answer", request.getBusinessId()));
		m.setBusinessUser(AuthenticationHelper.getCurrentPrincipal());
		m.setContext("integrationMetierDPerm");
		m.setBody(responseBody);

		esbValidator.validate(m);
		esbTemplate.send(m);
	}

	private ElementsIntegrationMetier parse(Source message) throws JAXBException, SAXException, IOException, EsbBusinessException {
		final Unmarshaller enveloppeUnmarshaller = jaxbContext.createUnmarshaller();
		enveloppeUnmarshaller.setSchema(getRequestSchemaEnveloppe());
		return (ElementsIntegrationMetier) enveloppeUnmarshaller.unmarshal(message);
	}

	private Schema getRequestSchemaEnveloppe() throws SAXException, IOException {
		if (schemaCache == null) {
			buildRequestSchemaEnveloppe();
		}
		return schemaCache;
	}

	private synchronized void buildRequestSchemaEnveloppe() throws SAXException, IOException {
		if (schemaCache == null) {
			final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			sf.setResourceResolver(new ClasspathCatalogResolver());
			final Source[] source = getClasspathSources("/event/dperm/typeSimpleDPerm-1.xsd", "/event/dperm/elementsIntegrationMetier-5.xsd");
			schemaCache = sf.newSchema(source);
		}
	}

	private static Source[] getClasspathSources(String... paths) throws IOException {
		final Source[] sources = new Source[paths.length];
		for (int i = 0 ; i < paths.length ; ++ i) {
			sources[i] = new StreamSource(new ClassPathResource(paths[i]).getURL().toExternalForm());
		}
		return sources;
	}
}
