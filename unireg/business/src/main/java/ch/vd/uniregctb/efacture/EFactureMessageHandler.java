package ch.vd.uniregctb.efacture;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.evd0025.v1.ObjectFactory;
import ch.vd.evd0025.v1.RegistrationRequestValidationRequest;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.jms.EsbMessageHelper;

public class EFactureMessageHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EFactureMessageHandler.class);

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private EFactureEventHandler handler;
	private HibernateTemplate hibernateTemplate;

	public void setHandler(EFactureEventHandler handler) {
		this.handler = handler;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		AuthenticationHelper.pushPrincipal("JMS-EvtEfacture");
		try {
			final String businessId = message.getBusinessId();
			LOGGER.info(String.format("Arrivée de l'événement de e-Facture '%s'", businessId));
			onMessage(message, EsbMessageHelper.extractCustomHeaders(message));
			hibernateTemplate.flush();  // Flush la session hibernate avant de poper le principal (sinon NullPointerException dans ModificationLogInterceptor)
		}
		catch (XmlException | JAXBException e) {
			// apparemment, l'XML est invalide... On va essayer de renvoyer une erreur propre quand même
			LOGGER.error(e.getMessage(), e);
			hibernateTemplate.flush();  // Flush la session hibernate avant de poper le principal (sinon NullPointerException dans ModificationLogInterceptor)
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			hibernateTemplate.flush();  // Flush la session hibernate avant de poper le principal (sinon NullPointerException dans ModificationLogInterceptor)
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage message, Map<String, String> incomingHeaders) throws Exception {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final Object event = u.unmarshal(message.getBodyAsSource());
		final Demande evt;
		if (event instanceof RegistrationRequestValidationRequest) {
			evt = new Demande(((RegistrationRequestValidationRequest) event).getRegistrationRequest());
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu : " + event.getClass());
		}
		handler.handle(evt);
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
			final ClassPathResource resource = handler.getRequestXSD();
			Source source = new StreamSource(resource.getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}
}
