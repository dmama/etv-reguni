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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersRequest;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersResponse;
import ch.vd.unireg.xml.event.party.v1.ExceptionResponse;
import ch.vd.unireg.xml.event.party.v1.ObjectFactory;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementHelper;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Classe de test du listener de requêtes de résolution d'adresses. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml",
		"classpath:ut/unireg-businessit-jms-evt-party.xml"
})
public class PartyNumbersRequestListenerItTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;

	private String INPUT_QUEUE;
	private String OUTPUT_QUEUE;
	private EsbMessageFactory esbMessageFactory;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());
		esbValidator.setSources(new Resource[]{new ClassPathResource("event/party/numbers-request-1.xsd"), new ClassPathResource("event/party/numbers-response-1.xsd")});
		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.party.service");
		OUTPUT_QUEUE = INPUT_QUEUE + ".response";

		EvenementHelper.clearQueue(esbTemplate, INPUT_QUEUE);
		EvenementHelper.clearQueue(esbTemplate, OUTPUT_QUEUE);
	}

	@Override
	public void onTearDown() throws Exception {
		popSecurityProvider();
		super.onTearDown();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testNumbersRequestUserWithoutAccessRight() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider();
		pushSecurityProvider(provider);

		final NumbersRequest request = new NumbersRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setIncludeCancelled(false);
		request.getTypes().add(PartyType.NATURAL_PERSON);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(INPUT_QUEUE, requestToString(request), OUTPUT_QUEUE);
				return null;
			}
		});

		try {
			parseResponse(getEsbMessage(OUTPUT_QUEUE));
			fail();
		}
		catch (ServiceException e) {
			assertInstanceOf(AccessDeniedExceptionInfo.class, e.getInfo());
			assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
		}
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testNumbersRequestOK() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		pushSecurityProvider(provider);

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
				addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, MockRue.Chamblon.RueDesUttins);
				return pp.getNumero();
			}
		});

		final NumbersRequest request = new NumbersRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setIncludeCancelled(false);
		request.getTypes().add(PartyType.NATURAL_PERSON);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(INPUT_QUEUE, requestToString(request), OUTPUT_QUEUE);
				return null;
			}
		});

		final EsbMessage message = getEsbMessage(OUTPUT_QUEUE);
		assertNotNull(message);

		final NumbersResponse response = parseResponse(message);
		assertNotNull(response);
		assertEquals(1, response.getIdsCount());

		final List<Integer> ids = parseIds(message);
		assertNotNull(ids);
		assertEquals(1, ids.size());
		assertEquals(id.intValue(), ids.get(0).intValue());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testNumbersRequestOkWithCustomHeader() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		pushSecurityProvider(provider);

		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
				addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, MockRue.Chamblon.RueDesUttins);
				return pp.getNumero();
			}
		});

		final NumbersRequest request = new NumbersRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setIncludeCancelled(false);
		request.getTypes().add(PartyType.NATURAL_PERSON);

		final String headerName = "spiritualFather";
		final String headerValue = "John Lanonne";

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage m = buildTextMessage(INPUT_QUEUE, requestToString(request), OUTPUT_QUEUE);
				m.addHeader(headerName, headerValue);
				esbTemplate.send(m);
				return null;
			}
		});

		final EsbMessage answer = getEsbMessage(OUTPUT_QUEUE);
		assertNotNull(answer);

		final String foundHeaderValue = answer.getHeader(headerName);
		assertEquals(headerValue, foundHeaderValue);
	}

	private static List<Integer> parseIds(EsbMessage message) throws Exception {
		final InputStream idsAsStream = message.getAttachmentAsStream("ids");
		if (idsAsStream == null) {
			return Collections.emptyList();
		}

		final List<Integer> ids = new ArrayList<Integer>();
		final Scanner scanner = new Scanner(idsAsStream, "UTF-8");
		try {
			while (scanner.hasNext()) {
				ids.add(Integer.parseInt(scanner.next()));
			}
		}
		finally {
			scanner.close();
		}
		return ids;
	}

	private EsbMessage buildTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		m.setServiceReplyTo(replyTo);
		return m;
	}

	protected void sendTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = buildTextMessage(queueName, texte, replyTo);
		esbTemplate.send(m);
	}

	private static String requestToString(NumbersRequest request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createRequest(request), out);
		return out.toString();
	}

	private EsbMessage getEsbMessage(String queue) throws Exception {
		esbTemplate.setReceiveTimeout(10000);        // On attend le message jusqu'à 10 secondes
		final EsbMessage msg = esbTemplate.receive(queue);
		assertNotNull("L'événement n'a pas été reçu.", msg);
		return msg;
	}

	private NumbersResponse parseResponse(EsbMessage message) throws Exception {

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());
		Schema schema = sf.newSchema(new Source[]{new StreamSource(new ClassPathResource("event/party/numbers-request-1.xsd").getURL().toExternalForm()),
				new StreamSource(new ClassPathResource("event/party/numbers-response-1.xsd").getURL().toExternalForm())});
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		final Object value = element.getValue();
		if (value instanceof ExceptionResponse) {
			throw new ServiceException(((ExceptionResponse) value).getExceptionInfo());
		}
		return (NumbersResponse) value;
	}
}
