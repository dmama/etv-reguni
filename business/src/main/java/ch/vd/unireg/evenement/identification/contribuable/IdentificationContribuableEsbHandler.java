package ch.vd.unireg.evenement.identification.contribuable;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.exception.ESBValidationException;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EsbMessageValidationHelper;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.jms.EsbMessageHandler;
import ch.vd.unireg.jms.EsbMessageHelper;
import ch.vd.unireg.jms.EsbMessageValidator;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.xml.ServiceException;

public class IdentificationContribuableEsbHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationContribuableEsbHandler.class);

	private EsbMessageValidator esbValidator;
	private EsbJmsTemplate esbTemplate;
	private ServiceTracing esbMessageValidatorServiceTracing;

	private Map<Class<?>, IdentificationContribuableRequestHandler<?,?>> handlers;
	private Schema schemaCache;
	private JAXBContext inputJaxbContext;

	public void setHandlers(Map<Class<?>, IdentificationContribuableRequestHandler<?,?>> handlers) {
		this.handlers = handlers;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbMessageValidatorServiceTracing(ServiceTracing esbMessageValidatorServiceTracing) {
		this.esbMessageValidatorServiceTracing = esbMessageValidatorServiceTracing;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		AuthenticationHelper.pushPrincipal("JMS-IdentificationCtb");
		try {
			onMessage(message);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage msg) throws Exception {
		try {
			LOGGER.info(String.format("Arrivée d'un événement (BusinessID = '%s')", msg.getBusinessId()));
			final Object req = parse(msg.getBodyAsSource());
			answer(handle(req, msg.getBusinessId()), msg);
		}
		catch (UnmarshalException e) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
		catch (ESBValidationException e) {
			throw new EsbBusinessException(EsbBusinessCode.REPONSE_IMPOSSIBLE, e.getMessage(), e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final List<Resource> resources = new ArrayList<>(handlers.size());
		for (IdentificationContribuableRequestHandler<?,?> handler : handlers.values()) {
			final List<ClassPathResource> resource = handler.getResponseXSD();
			resources.addAll(resource);
		}

		// Je ne sais pas trop pourquoi le fonctionnement ici est différent de ce qui a été fait dans {@link PartyRequestEsbHandler}
		// Toujours est-il que je n'ai réussi à faire fonctionner le "parse" qu'en mettant les classes ObjectFactory dans la liste...
		final Set<Class<?>> parsingClasses = new HashSet<>(handlers.size() * 2);
		for (Class<?> requestClass : handlers.keySet()) {
			try {
				final Class<?> objectFactoryClass = Class.forName(requestClass.getPackage().getName() + ".ObjectFactory");
				parsingClasses.add(objectFactoryClass);
			}
			catch (ClassNotFoundException e) {
				// pas très grave...
			}
			parsingClasses.add(requestClass);
		}

		inputJaxbContext = JAXBContext.newInstance(parsingClasses.toArray(new Class[parsingClasses.size()]));

		esbValidator = EsbMessageValidationHelper.buildValidator(esbMessageValidatorServiceTracing, new ClasspathCatalogResolver(), resources.toArray(new Resource[resources.size()]));
	}

	@SuppressWarnings("unchecked")
	private <REQ, RESP> JAXBElement<RESP> handle(REQ request, String businessId) throws ServiceException, EsbBusinessException {
		final IdentificationContribuableRequestHandler<REQ, RESP> handler = (IdentificationContribuableRequestHandler<REQ, RESP>) handlers.get(request.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("Aucun handler connu pour la requête [" + request.getClass() + ']');
		}

		return handler.handle(request, businessId);
	}

	private void answer(JAXBElement<?> response, EsbMessage requestMessage) throws ESBValidationException {
		try {
			final JAXBContext context = JAXBContext.newInstance(response.getValue() != null ? response.getValue().getClass() : null);
			final Marshaller marshaller = context.createMarshaller();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();

			marshaller.marshal(response, doc);
			EsbMessageHelper.cleanupDocumentNamespaceDefinitions(doc.getDocumentElement());

			if (LOGGER.isDebugEnabled()) {
				final StringWriter buffer = new StringWriter();
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				transformer.transform(new DOMSource(doc), new StreamResult(buffer));
				LOGGER.debug("Response body = [" + buffer.toString() + "]");
			}

			final EsbMessage m = EsbMessageFactory.createMessage(requestMessage);
			m.setBusinessId(requestMessage.getBusinessId() + "-answer");
			m.setBusinessUser("unireg");
			m.setContext("identification");
			m.setBody(doc);

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

	private Object parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = inputJaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(message);
		return element == null ? null : element.getValue();
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
			for (IdentificationContribuableRequestHandler<?,?> handler : handlers.values()) {
				final ClassPathResource resource = handler.getRequestXSD();
				sources.add(new StreamSource(resource.getURL().toExternalForm()));
			}
			schemaCache = sf.newSchema(sources.toArray(new Source[sources.size()]));
		}
	}
}
