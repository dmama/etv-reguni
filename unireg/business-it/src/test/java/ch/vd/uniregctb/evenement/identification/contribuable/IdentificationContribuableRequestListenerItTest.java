package ch.vd.uniregctb.evenement.identification.contribuable;

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

import org.junit.Test;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.validation.EsbXmlValidation;
import ch.vd.unireg.xml.event.identification.request.v2.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v2.ObjectFactory;
import ch.vd.unireg.xml.event.identification.response.v2.IdentificationContribuableResponse;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementHelper;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml",
		"classpath:ut/unireg-businessit-jms-evt-ident.xml"
})
public class IdentificationContribuableRequestListenerItTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;
	private String inputQueue;
	private String OutputQueue;
	private EsbXmlValidation esbValidator;
	private SmartLifecycle endpointManager;

	protected static String requestToString(IdentificationContribuableRequest request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createIdentificationContribuableRequest(request), out);
		return out.toString();
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");

		esbValidator = new EsbXmlValidation();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());
		esbValidator.setSources(new Resource[]{new ClassPathResource(getRequestXSD()), new ClassPathResource(getResponseXSD())});

		inputQueue = uniregProperties.getProperty("testprop.jms.queue.ident.ctb.input");
		OutputQueue = inputQueue + ".response";

		EvenementHelper.clearQueue(esbTemplate, inputQueue);
		EvenementHelper.clearQueue(esbTemplate, OutputQueue);

		endpointManager = getBean(SmartLifecycle.class, "jmsIdentCtbEndpointManagerAutomatique");
		endpointManager.start();
	}

	@Override
	public void onTearDown() throws Exception {
		endpointManager.stop();
		super.onTearDown();
	}

	String getRequestXSD() {
		return "event/identification/identification-contribuable-request-2.xsd";
	}

	String getResponseXSD() {
		return "event/identification/identification-contribuable-response-2.xsd";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
//	@Test
	public void testDemandeIdentificationAutomatiqueOK() throws Exception {

		final RegDate dateNaissance = RegDate.get(1982, 6);     // date partielle, pour le faire au moins une fois

		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier Vallard", dateNaissance, Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});
		globalTiersIndexer.schedule(id);
		globalTiersIndexer.sync();

		final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
		request.setNAVS13(7569396525489L);
		request.setNom("Monnier");
		request.setPrenoms("Christophe");

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);
		IdentificationContribuableResponse response = parseResponse(message);
		assertNull(response.getErreur());

		final IdentificationContribuableResponse.Contribuable infoCtb = response.getContribuable();
		assertNotNull(infoCtb);
		assertEquals(id, infoCtb.getNumeroContribuableIndividuel());
		assertEquals("Monnier Vallard", infoCtb.getNom());
		assertEquals("Christophe", infoCtb.getPrenom());
		assertEquals(dateNaissance, DataHelper.xmlToCore(infoCtb.getDateNaissance()));
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiquePlusieurs() throws Exception {

		final Long id1 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe","Monnier",date(1982,6,29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});
		final Long id2 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe","Monnier",date(1964,6,29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});

		globalTiersIndexer.schedule(id1);
		globalTiersIndexer.schedule(id2);

		globalTiersIndexer.sync();

		final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
		request.setNom("Monnier");
		request.setPrenoms("Christophe");

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);
		IdentificationContribuableResponse response = parseResponse(message);
		assertNotNull(response.getErreur());
		assertNull(response.getContribuable());
		assertNotNull(response.getErreur().getPlusieurs());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiqueAucun() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe","Monnier",date(1982,6,29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});

		globalTiersIndexer.schedule(id);
		globalTiersIndexer.sync();

		final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
		request.setNom("Adam");
		request.setPrenoms("Raphaël");

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);
		IdentificationContribuableResponse response = parseResponse(message);
		assertNotNull(response.getErreur());
		assertNull(response.getContribuable());
		assertNotNull(response.getErreur().getAucun());
	}

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
		getEsbTemplate().send(m);
	}

	protected EsbMessage getEsbMessage(String queue) throws Exception {
		getEsbTemplate().setReceiveTimeout(10000);        // On attend le message jusqu'à 10 secondes
		final EsbMessage msg = getEsbTemplate().receive(queue);
		assertNotNull("L'événement n'a pas été reçu.", msg);
		return msg;
	}

	IdentificationContribuableResponse parseResponse(EsbMessage message) throws Exception {

		final JAXBContext context = JAXBContext.newInstance(ch.vd.unireg.xml.event.identification.response.v2.ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());
		Schema schema = sf.newSchema(
				new Source[]{new StreamSource(new ClassPathResource(getRequestXSD()).getURL().toExternalForm()),
						new StreamSource(new ClassPathResource(getResponseXSD()).getURL().toExternalForm())});
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		final IdentificationContribuableResponse reponse = (IdentificationContribuableResponse)element.getValue();
		return reponse;
	}
}
