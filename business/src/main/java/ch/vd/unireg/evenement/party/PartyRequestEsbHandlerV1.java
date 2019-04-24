package ch.vd.unireg.evenement.party;

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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.technical.esb.util.exception.ESBValidationException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.EsbMessageValidationHelper;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.jms.EsbMessageHandler;
import ch.vd.unireg.jms.EsbMessageHelper;
import ch.vd.unireg.jms.EsbMessageValidator;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.event.party.v1.ObjectFactory;
import ch.vd.unireg.xml.event.party.v1.Request;
import ch.vd.unireg.xml.event.party.v1.Response;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;

/**
 * Listener qui écoute les requêtes de données de tiers et qui répond en conséquence.
 */
public class PartyRequestEsbHandlerV1 implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartyRequestEsbHandlerV1.class);

	private ServiceTracing esbMessageValidatorServiceTracing;
	private EsbJmsTemplate esbTemplate;
	private Map<Class<? extends Request>, RequestHandlerV1<? extends Request>> handlers;

	private final ObjectFactory objectFactory = new ObjectFactory();
	private EsbMessageValidator esbValidator;
	private Schema schemaCache;
	private JAXBContext inputJaxbContext;
	private JAXBContext outputJaxbContext;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandlers(Map<Class<? extends Request>, RequestHandlerV1<? extends Request>> handlers) {
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

		AuthenticationHelper.pushPrincipal("JMS-Party");
		try {
			onMessage(message);
		}
		catch (Exception e) {
			// toutes les erreurs levées ici sont des erreurs transientes ou des bugs
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private static EsbBusinessCode extractCode(ServiceExceptionInfo info) {
		if (info instanceof AccessDeniedExceptionInfo) {
			return EsbBusinessCode.DROITS_INSUFFISANTS;
		}
		if (info instanceof BusinessExceptionInfo) {
			final BusinessExceptionCode infoCode = BusinessExceptionCode.fromValue(((BusinessExceptionInfo) info).getCode());
			switch (infoCode) {
				case UNKNOWN_PARTY:
					return EsbBusinessCode.CTB_INEXISTANT;
				default:
					return EsbBusinessCode.REPONSE_IMPOSSIBLE;
			}
		}
		return EsbBusinessCode.REPONSE_IMPOSSIBLE;
	}

	private void onMessage(EsbMessage message) throws Exception {

		RequestHandlerResult<? extends Response> result;
		try {
			// on décode la requête
			final Request request = parse(message.getBodyAsSource());
			LOGGER.info(String.format("Arrivée d'un événement (BusinessID = '%s') %s", message.getBusinessId(), request));
			// on traite la requête
			result = handle(request);
		}
		catch (ServiceException e) {
			final ServiceExceptionInfo info = e.getInfo();
			final EsbBusinessCode code = extractCode(info);
			throw new EsbBusinessException(code, info.getMessage(), e);
		}
		catch (UnmarshalException e) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}

		// on répond
		try {
			answer(result.isValidable(), result.getResponse(), result.getAttachments(), message);
		}
		catch (ESBValidationException e) {
			LOGGER.error(e.getMessage(), e);
			throw new EsbBusinessException(EsbBusinessCode.REPONSE_IMPOSSIBLE, e.getMessage(), e);
		}
	}

	private Request parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = inputJaxbContext.createUnmarshaller();
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
			final LinkedHashSet<String> pathes = new LinkedHashSet<>();
			for (RequestHandlerV1 handler : handlers.values()) {
				//noinspection unchecked
				pathes.addAll(handler.getRequestXSDs());
			}
			schemaCache = sf.newSchema(XmlUtils.toSourcesArray(pathes));
		}
	}

	protected RequestHandlerResult<? extends Response> handle(Request request) throws ServiceException, EsbBusinessException {

		final RequestHandlerV1 handler = handlers.get(request.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("Aucun handler connu pour la requête [" + request.getClass() + ']');
		}

		//noinspection unchecked
		return handler.handle(request);
	}

	private void answer(boolean validateResponse, Response response, Map<String, EsbDataHandler> attachments, EsbMessage query) throws ESBValidationException {

		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();

			final Marshaller marshaller = outputJaxbContext.createMarshaller();
			marshaller.marshal(objectFactory.createResponse(response), doc);
			EsbMessageHelper.cleanupDocumentNamespaceDefinitions(doc.getDocumentElement());

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

			if (validateResponse) {
				esbValidator.validate(m);
			}
			esbTemplate.send(m);
		}
		catch (ESBValidationException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		final LinkedHashSet<String> pathes = new LinkedHashSet<>();
		for (RequestHandlerV1<?> handler : handlers.values()) {
			final List<String> resource = handler.getResponseXSDs();
			pathes.addAll(resource);
		}

		esbValidator = EsbMessageValidationHelper.buildValidator(esbMessageValidatorServiceTracing, null, XmlUtils.toResourcesArray(pathes));
		inputJaxbContext = JAXBContext.newInstance(handlers.keySet().toArray(new Class[0]));
		outputJaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}
}
