package ch.vd.unireg.evenement.rt;

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

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.event.rt.request.v1.MiseAJourRapportTravailRequest;
import ch.vd.unireg.xml.event.rt.request.v1.ObjectFactory;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementHelper;
import ch.vd.unireg.jms.EsbMessageValidator;

import static org.junit.Assert.assertNotNull;

/**
 * Factorise le code commun pour les autres classes concrètes du package
 */
@SuppressWarnings({"JavaDoc"})
abstract class RapportTravailRequestListenerItTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;
	private String inputQueue;
	private String OutputQueue;
	private EsbMessageValidator esbValidator;

	protected static String requestToString(MiseAJourRapportTravailRequest request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createMiseAJourRapportTravailRequest(request), out);
		return out.toString();
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");

		esbValidator = buildEsbMessageValidator(new Resource[]{new ClassPathResource(getRequestXSD()), new ClassPathResource(getResponseXSD())});

		inputQueue = uniregProperties.getProperty("testprop.jms.queue.rapportTravail.service");
		OutputQueue = inputQueue + ".response";

		EvenementHelper.clearQueue(esbTemplate, inputQueue, transactionManager);
		EvenementHelper.clearQueue(esbTemplate, OutputQueue, transactionManager);
	}

	abstract String getResponseXSD();

	abstract String getRequestXSD();


	EsbJmsTemplate getEsbTemplate() {
		return esbTemplate;
	}

	String getInputQueue() {
		return inputQueue;
	}

	String getOutputQueue() {
		return OutputQueue;
	}

	EsbMessage buildTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		m.setServiceReplyTo(replyTo);
		return m;
	}

	void sendTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = buildTextMessage(queueName, texte, replyTo);
		esbValidator.validate(m);
		EvenementHelper.sendMessage(esbTemplate, m, transactionManager);
	}

	protected EsbMessage getEsbMessage(String queue) throws Exception {
		final EsbMessage msg = EvenementHelper.getMessage(esbTemplate, queue, 10000, transactionManager);   // On attend le message jusqu'à 10 secondes
		assertNotNull("L'événement n'a pas été reçu.", msg);
		return msg;
	}

	MiseAJourRapportTravailResponse parseResponse(EsbMessage message) throws Exception {

		final JAXBContext context = JAXBContext.newInstance(ch.vd.unireg.xml.event.rt.response.v1.ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());
		Schema schema = sf.newSchema(
				new Source[]{new StreamSource(new ClassPathResource(getRequestXSD()).getURL().toExternalForm()),
				new StreamSource(new ClassPathResource(getResponseXSD()).getURL().toExternalForm())});
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		final MiseAJourRapportTravailResponse reponse = (MiseAJourRapportTravailResponse)element.getValue();
		return reponse;
	}
}
