package ch.vd.unireg.evenement.rapport.travail;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackException;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.exception.ESBValidationException;
import ch.vd.unireg.xml.event.rt.request.v1.MiseAJourRapportTravailRequest;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EsbMessageValidationHelper;
import ch.vd.unireg.jms.EsbMessageHandler;
import ch.vd.unireg.jms.EsbMessageValidator;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.xml.ServiceException;

//Listener qui écoute les demandes sur les rapports de travail pour le moment on a que des demandes de mise à jour
public class RapportTravailRequestEsbHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(RapportTravailRequestEsbHandler.class);

	private EsbMessageValidator esbValidator;
	private final ch.vd.unireg.xml.event.rt.response.v1.ObjectFactory objectFactory = new ch.vd.unireg.xml.event.rt.response.v1.ObjectFactory();

	private PlatformTransactionManager transactionManager;
	private EsbJmsTemplate esbTemplate;
	private RapportTravailRequestHandler rapportTravailRequestHandler;
	private ServiceTracing esbMessageValidatorServiceTracing;

	private Schema schemaCache;

	private JAXBContext inputJaxbContext;
	private JAXBContext outputJaxbContext;

	public void setRapportTravailRequestHandler(RapportTravailRequestHandler rapportTravailRequestHandler) {
		this.rapportTravailRequestHandler = rapportTravailRequestHandler;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbMessageValidatorServiceTracing(ServiceTracing esbMessageValidatorServiceTracing) {
		this.esbMessageValidatorServiceTracing = esbMessageValidatorServiceTracing;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		AuthenticationHelper.pushPrincipal("JMS-RapportTravail");
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

	private void onMessage(final EsbMessage message) throws Exception {

		MiseAJourRapportTravailResponse result;

		try {
			// on décode la requête
			final MiseAJourRapportTravailRequest request = parse(message.getBodyAsSource());
			LOGGER.info(String.format("Arrivée d'un événement (BusinessID = '%s') %s", message.getBusinessId(), request));

			// on traite la requête
			final MiseAjourRapportTravail miseAjourRapportTravail = MiseAjourRapportTravail.get(request, message.getBusinessId());

			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(false);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			try {
				result = template.execute(new TxCallback<MiseAJourRapportTravailResponse>() {
					@Override
					public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
						return rapportTravailRequestHandler.handle(miseAjourRapportTravail);
					}
				});
			}
			catch (TxCallbackException txe) {
				ServiceException e = (ServiceException) txe.getCause();
				LOGGER.error(e.getMessage(), e);
				result = new MiseAJourRapportTravailResponse();
				result.setExceptionInfo(e.getInfo());
			}
			catch (ValidationException e) {
				String msg = String.format("Exception de validation pour le message {businessId: %s}: Debiteur ou sourcier invalide dans Unireg.", message.getBusinessId());
				LOGGER.error(msg, e);
				result = new MiseAJourRapportTravailResponse();
				final BusinessExceptionInfo exceptionInfo = new BusinessExceptionInfo();
				exceptionInfo.setCode(BusinessExceptionCode.VALIDATION.value());
				exceptionInfo.setMessage(msg);
				result.setExceptionInfo(exceptionInfo);
			}

			result.setIdentifiantRapportTravail(request.getIdentifiantRapportTravail());
		}
		catch (UnmarshalException e) {
			final String msg = String.format("XML message {businessId: %s} is not valid (%s)", message.getBusinessId(), e.getMessage());
			LOGGER.error(msg, e);
			result = new MiseAJourRapportTravailResponse();
			final TechnicalExceptionInfo exceptionInfo = new TechnicalExceptionInfo();
			exceptionInfo.setMessage(msg);
			result.setExceptionInfo(exceptionInfo);
		}

		// on répond
		try {
			answer(result, message);
		}
		catch (ESBValidationException e) {
			LOGGER.error(e.getMessage(), e);
			answerValidationException(e, message);
		}
	}

	private void answerValidationException(ESBValidationException exception, EsbMessage message) throws ESBValidationException {
		final MiseAJourRapportTravailResponse result = new MiseAJourRapportTravailResponse();
		result.setExceptionInfo(new BusinessExceptionInfo(exception.getMessage(), BusinessExceptionCode.INVALID_RESPONSE.name(), null));
		answer(result, message);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		final List<Resource> resources = new ArrayList<>(1);
		final List<ClassPathResource> resource = rapportTravailRequestHandler.getResponseXSD();
		resources.addAll(resource);

		esbValidator = EsbMessageValidationHelper.buildValidator(esbMessageValidatorServiceTracing, new ClasspathCatalogResolver(), resources.toArray(new Resource[resources.size()]));

		outputJaxbContext = JAXBContext.newInstance(ch.vd.unireg.xml.event.rt.response.v1.ObjectFactory.class.getPackage().getName());
		inputJaxbContext = JAXBContext.newInstance(ch.vd.unireg.xml.event.rt.request.v1.ObjectFactory.class.getPackage().getName());
	}

	private MiseAJourRapportTravailRequest parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = inputJaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(message);
		return element == null ? null : (MiseAJourRapportTravailRequest) element.getValue();
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
			final ClassPathResource resource = rapportTravailRequestHandler.getRequestXSD();
			final Source source = new StreamSource(resource.getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}

	private void answer(MiseAJourRapportTravailResponse response, EsbMessage query) throws ESBValidationException {

		try {
			final Marshaller marshaller = outputJaxbContext.createMarshaller();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();

			marshaller.marshal(objectFactory.createMiseAJourRapportTravailResponse(response), doc);

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
			m.setContext("rapportTravail");
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
}
