package ch.vd.uniregctb.efacture;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.evd0025.v1.ObjectFactory;
import ch.vd.evd0025.v1.RegistrationRequestValidationRequest;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.EsbMessageHelper;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

public class EFactureEventListener extends EsbMessageEndpointListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EFactureEventListener.class);

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	private Schema schemaCache;

	private EFactureEventHandler handler;

	public void setHandler(EFactureEventHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		nbMessagesRecus.incrementAndGet();

		AuthenticationHelper.pushPrincipal("JMS-EvtEfacture");
		try {
			final String businessId = message.getBusinessId();
			LOGGER.info(String.format("Arrivée de l'événement de e-Facture '%s'", businessId));
			onMessage(message, EsbMessageHelper.extractCustomHeaders(message));
		}
		catch (XmlException e) {
			// apparemment, l'XML est invalide... On va essayer de renvoyer une erreur propre quand même
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(message, e.getMessage(), e, ErrorType.TECHNICAL, "");
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage message, Map<String, String> incomingHeaders) throws Exception {
		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final Object event = u.unmarshal(message.getBodyAsSource());
		final EFactureEvent evt;
		if (event instanceof RegistrationRequestValidationRequest) {
			evt = new DemandeValidationInscription(((RegistrationRequestValidationRequest) event).getRegistrationRequest());
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

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}
