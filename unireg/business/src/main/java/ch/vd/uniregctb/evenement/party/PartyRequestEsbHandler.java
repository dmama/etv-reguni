package ch.vd.uniregctb.evenement.party;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.technical.esb.util.exception.ESBValidationException;
import ch.vd.technical.esb.validation.EsbXmlValidation;
import ch.vd.unireg.xml.event.party.v1.ExceptionResponse;
import ch.vd.unireg.xml.event.party.v1.ObjectFactory;
import ch.vd.unireg.xml.event.party.v1.Request;
import ch.vd.unireg.xml.event.party.v1.Response;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.xml.ServiceException;

/**
 * Listener qui écoute les requêtes de données de tiers et qui répond en conséquence.
 */
public class PartyRequestEsbHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(PartyRequestEsbHandler.class);

	private EsbXmlValidation esbValidator;
	private EsbJmsTemplate esbTemplate;
	private final ObjectFactory objectFactory = new ObjectFactory();

	private Map<Class<? extends Request>, RequestHandler> handlers;
	private Schema schemaCache;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandlers(Map<Class<? extends Request>, RequestHandler> handlers) {
		this.handlers = handlers;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		AuthenticationHelper.pushPrincipal("JMS-Party");
		try {
			onMessage(message);
		}
		catch (Exception e) {
			// toutes les erreurs levées ici sont des erreurs transientes ou des bugs
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage message) throws Exception {

		RequestHandlerResult result;
		try {
			// on décode la requête
			final Request request = parse(message.getBodyAsSource());
			LOGGER.info(String.format("Arrivée d'un événement (BusinessID = '%s') %s", message.getBusinessId(), request));
			// on traite la requête
			result = handle(request);
		}
		catch (ServiceException e) {
			final ExceptionResponse r = new ExceptionResponse();
			r.setExceptionInfo(e.getInfo());
			result = new RequestHandlerResult(r);
		}
		catch (UnmarshalException e) {
			String msg = String.format("UnmarshalException raised in Unireg. XML message {businessId: %s} is not valid", message.getBusinessId());
			LOGGER.error(msg, e);
			final ExceptionResponse r = new ExceptionResponse(new TechnicalExceptionInfo(msg, null));
			result = new RequestHandlerResult(r);
		}

		// on répond
		try {
			answer(result.getResponse(), result.getAttachments(), message);
		}
		catch (ESBValidationException e) {
			LOGGER.error(e, e);
			answerValidationException(e, result.getAttachments(), message);
		}
	}

	private Request parse(Source message) throws JAXBException, SAXException, IOException {
		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(message);
		return element == null ? null : (Request) element.getValue();
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
			final List<Source> sources = new ArrayList<>(handlers.size());
			for (RequestHandler handler : handlers.values()) {
				final ClassPathResource resource = handler.getRequestXSD();
				sources.add(new StreamSource(resource.getURL().toExternalForm()));
			}
			schemaCache = sf.newSchema(sources.toArray(new Source[sources.size()]));
		}
	}

	protected RequestHandlerResult handle(Request request) throws ServiceException {

		final RequestHandler handler = handlers.get(request.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("Aucun handler connu pour la requête [" + request.getClass() + ']');
		}

		//noinspection unchecked
		return handler.handle(request);
	}

	private void answer(Response response, Map<String, EsbDataHandler> attachments, EsbMessage query) throws ESBValidationException {

		try {
			final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			final Marshaller marshaller = context.createMarshaller();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();

			marshaller.marshal(objectFactory.createResponse(response), doc);

			if (LOGGER.isDebugEnabled()) {
				StringWriter buffer = new StringWriter();
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				transformer.transform(new DOMSource(doc), new StreamResult(buffer));
				LOGGER.debug("Response body = [" + buffer.toString() + "]");
			}

			final EsbMessage m = EsbMessageFactory.createMessage(query);
			m.setBusinessId(query.getBusinessId() + "-answer");
			m.setBusinessUser("unireg");
			m.setContext("party");
			m.setBody(doc);

			// les attachements éventuels
			if (attachments != null) {
				for (Map.Entry<String, EsbDataHandler> entry : attachments.entrySet()) {
					m.addAttachment(entry.getKey(), entry.getValue());
				}
			}

			esbValidator.validate(m);
			esbTemplate.send(m);
		}
		catch (ESBValidationException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void answerValidationException(ESBValidationException exception, Map<String, EsbDataHandler> attachments, EsbMessage message) throws ESBValidationException {

		final ExceptionResponse er = new ExceptionResponse();
		er.setExceptionInfo(new BusinessExceptionInfo(exception.getMessage(), BusinessExceptionCode.INVALID_RESPONSE.name(), null));

		answer(er, attachments, message);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		final List<Resource> resources = new ArrayList<>(handlers.size());
		for (RequestHandler handler : handlers.values()) {
			final List<ClassPathResource> resource = handler.getResponseXSD();
			resources.addAll(resource);
		}

		esbValidator = new EsbXmlValidation();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());
		esbValidator.setSources(resources.toArray(new Resource[resources.size()]));
	}
}
