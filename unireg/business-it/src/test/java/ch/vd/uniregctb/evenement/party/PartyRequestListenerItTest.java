package ch.vd.uniregctb.evenement.party;

import javax.jms.ConnectionFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.address.v1.AddressRequest;
import ch.vd.unireg.xml.event.party.address.v1.AddressResponse;
import ch.vd.unireg.xml.event.party.v1.ExceptionResponse;
import ch.vd.unireg.xml.event.party.v1.ObjectFactory;
import ch.vd.unireg.xml.event.party.v1.Request;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Classe de test du listener de requêtes de résolution d'adresses. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml"
})
public class PartyRequestListenerItTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;

	private String INPUT_QUEUE;
	private String OUTPUT_QUEUE;
	private DefaultMessageListenerContainer container;
	private EsbMessageFactory esbMessageFactory;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final ConnectionFactory connectionFactory = getBean(ConnectionFactory.class, "jmsConnectionFactory");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");

		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());
		esbValidator.setSources(new Resource[]{new ClassPathResource("event/party/address-request-1.xsd"), new ClassPathResource("event/party/address-response-1.xsd")});
		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.party.service");
		OUTPUT_QUEUE = INPUT_QUEUE + ".response";

		clearQueue(INPUT_QUEUE);
		clearQueue(OUTPUT_QUEUE);

		final AddressRequestHandler handler = new AddressRequestHandler();
		handler.setTiersDAO(tiersDAO);
		handler.setAdresseService(adresseService);

		final Map<Class<? extends Request>, PartyRequestHandler> handlers = new HashMap<Class<? extends Request>, PartyRequestHandler>();
		handlers.put(AddressRequest.class, handler);

		final PartyRequestListener listener = new PartyRequestListener();
		listener.setEsbTemplate(esbTemplate);
		listener.setHandlers(handlers);
		listener.afterPropertiesSet();

		container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setTransactionManager(transactionManager);
		container.setMessageListener(listener);
		container.setDestinationName(INPUT_QUEUE);
		container.afterPropertiesSet();
		container.start();
	}

	@Override
	public void onTearDown() throws Exception {
		container.destroy();
		popSecurityProvider();
	}

	protected void clearQueue(String queueName) throws Exception {
		while (esbTemplate.receive(queueName) != null) {
		}
	}

	@NotTransactional
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestUserWithoutAccessRight() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider();
		pushSecurityProvider(provider);

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(222);
		request.getTypes().add(AddressType.RESIDENCE);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(INPUT_QUEUE, requestToString(request), OUTPUT_QUEUE);
				return null;
			}
		});

		try {
			getResponse(OUTPUT_QUEUE);
			fail();
		}
		catch (ServiceException e) {
			assertInstanceOf(AccessDeniedExceptionInfo.class, e.getInfo());
			assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
		}
	}

	@NotTransactional
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestOnProtectedFolder() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		provider.setDossiersProteges(222L);
		pushSecurityProvider(provider);

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(222);
		request.getTypes().add(AddressType.RESIDENCE);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(INPUT_QUEUE, requestToString(request), OUTPUT_QUEUE);
				return null;
			}
		});

		try {
			getResponse(OUTPUT_QUEUE);
			fail();
		}
		catch (ServiceException e) {
			assertInstanceOf(AccessDeniedExceptionInfo.class, e.getInfo());
			assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture sur le tiers n° 222.", e.getMessage());
		}
	}

	@NotTransactional
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestOnUnknownParty() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		pushSecurityProvider(provider);

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(222);
		request.getTypes().add(AddressType.RESIDENCE);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(INPUT_QUEUE, requestToString(request), OUTPUT_QUEUE);
				return null;
			}
		});

		try {
			getResponse(OUTPUT_QUEUE);
			fail();
		}
		catch (ServiceException e) {
			assertInstanceOf(BusinessExceptionInfo.class, e.getInfo());
			assertEquals("Le tiers n°222 n'existe pas.", e.getMessage());
		}
	}

	@NotTransactional
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestOK() throws Exception {

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

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(id.intValue());
		request.getTypes().add(AddressType.RESIDENCE);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(INPUT_QUEUE, requestToString(request), OUTPUT_QUEUE);
				return null;
			}
		});

		final AddressResponse response = getResponse(OUTPUT_QUEUE);
		assertNotNull(response);

		final List<Address> addresses = response.getAddresses();
		assertNotNull(addresses);
		assertEquals(1, addresses.size());

		final Address address = addresses.get(0);
		assertNotNull(address);

		assertEquals(new Date(1950, 3, 14), address.getDateFrom());
		assertNull(address.getDateTo());
		assertEquals(AddressType.RESIDENCE, address.getType());

		final PersonMailAddressInfo person = address.getPerson();
		assertNotNull(person);
		assertEquals("2", person.getMrMrs());
		assertEquals("Michel", person.getFirstName());
		assertEquals("Mabelle", person.getLastName());
		assertEquals("Monsieur", person.getSalutation());
		assertEquals("Monsieur", person.getFormalGreeting());

		final AddressInformation info = address.getAddressInformation();
		assertNotNull(info);
		assertEquals("Les Uttins", info.getStreet());
		assertEquals("Chamblon", info.getTown());
		assertEquals(Long.valueOf(1436), info.getSwissZipCode());
		assertEquals(Integer.valueOf(5876), info.getSwissZipCodeId());
		assertEquals("CH", info.getCountry());
		assertEquals("Suisse", info.getCountryName());
		assertEquals(Integer.valueOf(198539), info.getStreetId());
		assertEquals(Integer.valueOf(8100), info.getCountryId());
		assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());

		final FormattedAddress formatted = address.getFormattedAddress();
		assertNotNull(formatted);
		assertEquals("Monsieur", formatted.getLine1());
		assertEquals("Michel Mabelle", formatted.getLine2());
		assertEquals("Les Uttins", formatted.getLine3());
		assertEquals("1436 Chamblon", formatted.getLine4());
		assertNull(formatted.getLine5());
	}

	protected void assertTextMessage(String queueName, final String texte) throws Exception {

		esbTemplate.setReceiveTimeout(10000);        // On attend le message jusqu'à 10 secondes
		final EsbMessage msg = esbTemplate.receive(queueName);
		assertNotNull("L'événement n'a pas été reçu.", msg);
		String actual = msg.getBodyAsString();
		actual = actual.replaceAll(" standalone=\"(no|yes)\"", ""); // on ignore l'attribut standalone s'il existe
		assertEquals(texte, actual);

		final EsbMessage noMsg = esbTemplate.receive(queueName);
		assertNull(noMsg);
	}

	protected void sendTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		m.setServiceReplyTo(replyTo);

		esbTemplate.send(m);
	}

	private static String requestToString(AddressRequest request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createRequest(request), out);
		return out.toString();
	}

	private AddressResponse getResponse(String queue) throws Exception {

		esbTemplate.setReceiveTimeout(10000);        // On attend le message jusqu'à 10 secondes
		final EsbMessage msg = esbTemplate.receive(queue);
		assertNotNull("L'événement n'a pas été reçu.", msg);

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());
		Schema schema = sf.newSchema(new Source[]{new StreamSource(new ClassPathResource("event/party/address-request-1.xsd").getURL().toExternalForm()),
				new StreamSource(new ClassPathResource("event/party/address-response-1.xsd").getURL().toExternalForm())});
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(msg.getBodyAsSource());
		final Object value = element.getValue();
		if (value instanceof ExceptionResponse) {
			throw new ServiceException(((ExceptionResponse) value).getExceptionInfo());
		}
		return (AddressResponse) value;
	}
}
