package ch.vd.uniregctb.evenement.party;

import javax.jms.ConnectionFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
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
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.address.v1.AddressRequest;
import ch.vd.unireg.xml.event.party.v1.ObjectFactory;
import ch.vd.unireg.xml.event.party.v1.Request;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.party.request");
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

		// On attend le message
		final String response =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<ns5:response " +
						"xmlns:ns5=\"http://www.vd.ch/unireg/event/party/response/1\" " +
						"xmlns=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/event/party/request/1\" " +
						"xmlns:ns3=\"http://www.vd.ch/unireg/event/party/address-request/1\" " +
						"xmlns:ns4=\"http://www.vd.ch/unireg/exception/1\" " +
						"xmlns:ns6=\"http://www.vd.ch/unireg/party/address/1\" " +
						"xmlns:ns7=\"http://www.ech.ch/xmlns/eCH-0010/4\" " +
						"xmlns:ns8=\"http://www.vd.ch/unireg/event/party/address-response/1\" " +
						"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
						"xsi:type=\"ns5:exceptionResponseType\">" +
						"<ns5:exceptionInfo xsi:type=\"ns4:accessDeniedExceptionInfoType\">" +
						"<ns4:message>L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.</ns4:message>" +
						"</ns5:exceptionInfo>" +
						"</ns5:response>";

		assertTextMessage(OUTPUT_QUEUE, response);
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

		// On attend le message
		final String response =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<ns5:response " +
						"xmlns:ns5=\"http://www.vd.ch/unireg/event/party/response/1\" " +
						"xmlns=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/event/party/request/1\" " +
						"xmlns:ns3=\"http://www.vd.ch/unireg/event/party/address-request/1\" " +
						"xmlns:ns4=\"http://www.vd.ch/unireg/exception/1\" " +
						"xmlns:ns6=\"http://www.vd.ch/unireg/party/address/1\" " +
						"xmlns:ns7=\"http://www.ech.ch/xmlns/eCH-0010/4\" " +
						"xmlns:ns8=\"http://www.vd.ch/unireg/event/party/address-response/1\" " +
						"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
						"xsi:type=\"ns5:exceptionResponseType\">" +
						"<ns5:exceptionInfo xsi:type=\"ns4:accessDeniedExceptionInfoType\">" +
						"<ns4:message>L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture sur le tiers n° 222.</ns4:message>" +
						"</ns5:exceptionInfo>" +
						"</ns5:response>";

		assertTextMessage(OUTPUT_QUEUE, response);
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

		// On attend le message
		final String response =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<ns5:response " +
						"xmlns:ns5=\"http://www.vd.ch/unireg/event/party/response/1\" " +
						"xmlns=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/event/party/request/1\" " +
						"xmlns:ns3=\"http://www.vd.ch/unireg/event/party/address-request/1\" " +
						"xmlns:ns4=\"http://www.vd.ch/unireg/exception/1\" " +
						"xmlns:ns6=\"http://www.vd.ch/unireg/party/address/1\" " +
						"xmlns:ns7=\"http://www.ech.ch/xmlns/eCH-0010/4\" " +
						"xmlns:ns8=\"http://www.vd.ch/unireg/event/party/address-response/1\" " +
						"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
						"xsi:type=\"ns5:exceptionResponseType\">" +
						"<ns5:exceptionInfo xsi:type=\"ns4:businessExceptionInfoType\">" +
						"<ns4:message>Le tiers n°222 n'existe pas.</ns4:message>" +
						"<ns4:code>UNKNOWN_PARTY</ns4:code>" +
						"</ns5:exceptionInfo>" +
						"</ns5:response>";

		assertTextMessage(OUTPUT_QUEUE, response);
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

		// On attend le message
		final String response =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<ns5:response " +
						"xmlns:ns5=\"http://www.vd.ch/unireg/event/party/response/1\" " +
						"xmlns=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/event/party/request/1\" " +
						"xmlns:ns3=\"http://www.vd.ch/unireg/event/party/address-request/1\" " +
						"xmlns:ns4=\"http://www.vd.ch/unireg/exception/1\" " +
						"xmlns:ns6=\"http://www.vd.ch/unireg/party/address/1\" " +
						"xmlns:ns7=\"http://www.ech.ch/xmlns/eCH-0010/4\" " +
						"xmlns:ns8=\"http://www.vd.ch/unireg/event/party/address-response/1\" " +
						"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
						"xsi:type=\"ns8:addressResponseType\">" +
						"<ns8:addresses>" +
						"<ns6:dateFrom><year>1950</year><month>3</month><day>14</day></ns6:dateFrom>" +
						"<ns6:person>" +
						"<ns7:mrMrs>2</ns7:mrMrs>" +
						"<ns7:firstName>Michel</ns7:firstName>" +
						"<ns7:lastName>Mabelle</ns7:lastName>" +
						"<ns6:salutation>Monsieur</ns6:salutation>" +
						"<ns6:formalGreeting>Monsieur</ns6:formalGreeting>" +
						"</ns6:person>" +
						"<ns6:addressInformation>" +
						"<ns7:street>Les Uttins</ns7:street>" +
						"<ns7:town>Chamblon</ns7:town>" +
						"<ns7:swissZipCode>1436</ns7:swissZipCode>" +
						"<ns7:swissZipCodeId>5876</ns7:swissZipCodeId>" +
						"<ns7:country>CH</ns7:country>" +
						"<ns6:countryName>Suisse</ns6:countryName>" +
						"<ns6:streetId>198539</ns6:streetId>" +
						"<ns6:countryId>8100</ns6:countryId>" +
						"<ns6:tariffZone>SWITZERLAND</ns6:tariffZone>" +
						"</ns6:addressInformation>" +
						"<ns6:formattedAddress>" +
						"<ns6:line1>Monsieur</ns6:line1>" +
						"<ns6:line2>Michel Mabelle</ns6:line2>" +
						"<ns6:line3>Les Uttins</ns6:line3>" +
						"<ns6:line4>1436 Chamblon</ns6:line4>" +
						"</ns6:formattedAddress>" +
						"<ns6:type>RESIDENCE</ns6:type>" +
						"</ns8:addresses>" +
						"</ns5:response>";

		assertTextMessage(OUTPUT_QUEUE, response);
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
}
