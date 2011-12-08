package ch.vd.uniregctb.evenement.party;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.TransactionalEsbMessageListener;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.unireg.xml.event.party.address.v1.AddressRequest;
import ch.vd.unireg.xml.event.party.v1.ExceptionResponse;
import ch.vd.unireg.xml.event.party.v1.ObjectFactory;
import ch.vd.unireg.xml.event.party.v1.Request;
import ch.vd.unireg.xml.event.party.v1.Response;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.MonitorableMessageListener;
import ch.vd.uniregctb.xml.ServiceException;

/**
 * Listener qui écoute les requêtes de données de tiers et qui répond en conséquence.
 */
public class PartyRequestListener extends TransactionalEsbMessageListener implements MonitorableMessageListener, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(PartyRequestListener.class);

	private EsbMessageFactory esbMessageFactory;
	private final ObjectFactory objectFactory = new ObjectFactory();
	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	private Map<Class<? extends Request>, PartyRequestHandler> handlers;
	private Schema schemaCache;

	public void setHandlers(Map<Class<? extends Request>, PartyRequestHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		nbMessagesRecus.incrementAndGet();

		AuthenticationHelper.pushPrincipal("JMS-Party");
		try {
			onMessage(message);
		}
		catch (Exception e) {
			// toutes les erreurs levées ici sont des erreurs transientes ou de validation du XML
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage message) throws Exception {

		// on décode la requête
		final Request request = parse(message.getBodyAsSource());

		LOGGER.info(String.format("Arrivée d'un événement %s", request));

		// on traite la requête
		Response response;
		try {
			response = handle(request);
		}
		catch (ServiceException e) {
			final ExceptionResponse r = new ExceptionResponse();
			r.setExceptionInfo(e.getInfo());
			response = r;
		}

		// on répond
		answer(response, message);
	}

	private Request parse(Source message) throws JAXBException, SAXException, IOException {
		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(message);
		return element == null ? null : (AddressRequest) element.getValue();
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
			final List<Source> sources = new ArrayList<Source>(handlers.size());
			for (PartyRequestHandler handler : handlers.values()) {
				final ClassPathResource resource = handler.getRequestXSD();
				sources.add(new StreamSource(resource.getURL().toExternalForm()));
			}
			schemaCache = sf.newSchema(sources.toArray(new Source[sources.size()]));
		}
	}

	protected Response handle(Request request) throws ServiceException {

		final PartyRequestHandler handler = handlers.get(request.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("Aucun handler connu pour la requête [" + request.getClass() + ']');
		}

		//noinspection unchecked
		return handler.handle(request);
	}

	private void answer(Response response, EsbMessage query) {

		try {
			final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			final Marshaller marshaller = context.createMarshaller();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();

			marshaller.marshal(objectFactory.createResponse(response), doc);

			final EsbMessage m = esbMessageFactory.createMessage(query);
			m.setBusinessId(query.getBusinessId() + "-answer");
			m.setBusinessUser("unireg");
			m.setContext("party");
			m.setBody(doc);

			esbTemplate.send(m);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		final List<Resource> resources = new ArrayList<Resource>(handlers.size());
		for (PartyRequestHandler handler : handlers.values()) {
			final ClassPathResource resource = handler.getResponseXSD();
			resources.add(resource);
		}

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());
		esbValidator.setSources(resources.toArray(new Resource[resources.size()]));

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);
	}
}
