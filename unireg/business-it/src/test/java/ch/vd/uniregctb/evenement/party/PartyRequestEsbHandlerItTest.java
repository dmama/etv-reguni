package ch.vd.uniregctb.evenement.party;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.validation.EsbXmlValidation;
import ch.vd.unireg.xml.event.party.v1.ExceptionResponse;
import ch.vd.unireg.xml.event.party.v1.ObjectFactory;
import ch.vd.unireg.xml.event.party.v1.Request;
import ch.vd.unireg.xml.event.party.v1.Response;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementHelper;
import ch.vd.uniregctb.jms.EsbBusinessErrorCollector;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertNotNull;

/**
 * Factorise le code commun pour les autres classes concrètes du package
 */
@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml",
		"classpath:ut/unireg-businessit-jms-evt-party.xml",
		"classpath:ut/unireg-businessit-security.xml"
})
abstract class PartyRequestEsbHandlerItTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;
	private String inputQueue;
	private String OutputQueue;
	private EsbXmlValidation esbValidator;
	private EsbBusinessErrorCollector errorCollector;

	protected static String requestToString(Request request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createRequest(request), out);
		return out.toString();
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");
		errorCollector = getBean(EsbBusinessErrorCollector.class, "partyRequestErrorCollector");
		errorCollector.clear();

		esbValidator = new EsbXmlValidation();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());

		final List<Resource> sources = new ArrayList<>();
		sources.add(new ClassPathResource(getRequestXSD()));
		for (String xsd : getResponseXSD()) {
			sources.add(new ClassPathResource(xsd));
		}
		esbValidator.setSources(sources.toArray(new Resource[sources.size()]));

		inputQueue = uniregProperties.getProperty("testprop.jms.queue.party.service");
		OutputQueue = inputQueue + ".response";

		EvenementHelper.clearQueue(esbTemplate, inputQueue);
		EvenementHelper.clearQueue(esbTemplate, OutputQueue);
	}

	protected abstract List<String> getResponseXSD();

	protected abstract String getRequestXSD();

	protected void deactivateEsbValidator() {
		esbValidator = null;
	}

	protected EsbJmsTemplate getEsbTemplate() {
		return esbTemplate;
	}

	protected String getInputQueue() {
		return inputQueue;
	}

	protected String getOutputQueue() {
		return OutputQueue;
	}

	protected EsbBusinessErrorCollector getErrorCollector() {
		return errorCollector;
	}

	protected EsbMessage buildTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		m.setServiceReplyTo(replyTo);
		return m;
	}

	/**
	 * @return le businessId du message envoyé
	 */
	protected String sendTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = buildTextMessage(queueName, texte, replyTo);
		validateMessage(m);
		getEsbTemplate().send(m);
		return m.getBusinessId();
	}

	protected void validateMessage(EsbMessage msg) throws Exception {
		if (esbValidator != null) {
			esbValidator.validate(msg);
		}
	}

	protected EsbMessage getEsbMessage(String queue) throws Exception {
		getEsbTemplate().setReceiveTimeout(10000);        // On attend le message jusqu'à 10 secondes
		final EsbMessage msg = getEsbTemplate().receive(queue);
		assertNotNull("L'événement n'a pas été reçu.", msg);
		return msg;
	}

	protected Response parseResponse(EsbMessage message) throws Exception {

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());

		final List<Source> sources = new ArrayList<>();
		sources.add(new StreamSource(new ClassPathResource(getRequestXSD()).getURL().toExternalForm()));
		for (String xsd : getResponseXSD()) {
			sources.add(new StreamSource(new ClassPathResource(xsd).getURL().toExternalForm()));
		}
		final Schema schema = sf.newSchema(sources.toArray(new Source[sources.size()]));
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		final Object value = element.getValue();
		if (value instanceof ExceptionResponse) {
			throw new ServiceException(((ExceptionResponse) value).getExceptionInfo());
		}
		return (Response) value;
	}
}
