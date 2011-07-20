package ch.vd.uniregctb.evenement.adresses;

import javax.jms.ConnectionFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

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
import ch.vd.unireg.xml.address.AddressType;
import ch.vd.unireg.xml.common.UserLogin;
import ch.vd.unireg.xml.event.address.GetAddressRequest;
import ch.vd.unireg.xml.event.address.ObjectFactory;
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
public class GetAdressesRequestListenerItTest extends BusinessItTest {

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
		esbValidator.setSources(new Resource[]{new ClassPathResource("event/address/get-address-request-1.xsd"), new ClassPathResource("event/address/get-address-response-1.xsd")});
		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.party.service");
		OUTPUT_QUEUE = INPUT_QUEUE + ".response";

		clearQueue(INPUT_QUEUE);
		clearQueue(OUTPUT_QUEUE);

		final GetAdressesRequestListener listener = new GetAdressesRequestListener();
		listener.setEsbTemplate(esbTemplate);
		listener.setEsbMessageFactory(esbMessageFactory);
		listener.setTiersDAO(tiersDAO);
		listener.setAdresseService(adresseService);

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
	public void testGetAddressUserNoAccessRight() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider();
		pushSecurityProvider(provider);

		final GetAddressRequest request = new GetAddressRequest();
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
						"<ns6:response " +
						"xmlns:ns6=\"http://www.vd.ch/unireg/event/get-address-response/1\" " +
						"xmlns=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/event/request/1\" " +
						"xmlns:ns3=\"http://www.vd.ch/unireg/event/get-address-request/1\" " +
						"xmlns:ns4=\"http://www.vd.ch/unireg/address/1\" " +
						"xmlns:ns5=\"http://www.ech.ch/xmlns/eCH-0010/4\" " +
						"xmlns:ns7=\"http://www.vd.ch/unireg/exception/1\">" +
						"<ns6:exceptionInfo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ns7:accessDeniedExceptionInfoType\">" +
						"<ns7:message>L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.</ns7:message>" +
						"</ns6:exceptionInfo>" +
						"</ns6:response>";

		assertTextMessage(OUTPUT_QUEUE, response);
	}

	@NotTransactional
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testGetAddressUserProtectedFolder() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		provider.setDossiersProteges(222L);
		pushSecurityProvider(provider);

		final GetAddressRequest request = new GetAddressRequest();
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
						"<ns6:response " +
						"xmlns:ns6=\"http://www.vd.ch/unireg/event/get-address-response/1\" " +
						"xmlns=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/event/request/1\" " +
						"xmlns:ns3=\"http://www.vd.ch/unireg/event/get-address-request/1\" " +
						"xmlns:ns4=\"http://www.vd.ch/unireg/address/1\" " +
						"xmlns:ns5=\"http://www.ech.ch/xmlns/eCH-0010/4\" " +
						"xmlns:ns7=\"http://www.vd.ch/unireg/exception/1\">" +
						"<ns6:exceptionInfo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ns7:accessDeniedExceptionInfoType\">" +
						"<ns7:message>L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture sur le tiers n° 222.</ns7:message>" +
						"</ns6:exceptionInfo>" +
						"</ns6:response>";

		assertTextMessage(OUTPUT_QUEUE, response);
	}

	@NotTransactional
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testGetAddressPartyUnknown() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		pushSecurityProvider(provider);

		final GetAddressRequest request = new GetAddressRequest();
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
						"<ns6:response " +
						"xmlns:ns6=\"http://www.vd.ch/unireg/event/get-address-response/1\" " +
						"xmlns=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/event/request/1\" " +
						"xmlns:ns3=\"http://www.vd.ch/unireg/event/get-address-request/1\" " +
						"xmlns:ns4=\"http://www.vd.ch/unireg/address/1\" " +
						"xmlns:ns5=\"http://www.ech.ch/xmlns/eCH-0010/4\" " +
						"xmlns:ns7=\"http://www.vd.ch/unireg/exception/1\">" +
						"<ns6:exceptionInfo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ns7:businessExceptionInfoType\">" +
						"<ns7:message>Le tiers n°222 n'existe pas.</ns7:message>" +
						"<ns7:code>UNKNOWN_PARTY</ns7:code>" +
						"</ns6:exceptionInfo>" +
						"</ns6:response>";

		assertTextMessage(OUTPUT_QUEUE, response);
	}

	@NotTransactional
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testGetAddressPartyOK() throws Exception {

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

		final GetAddressRequest request = new GetAddressRequest();
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
						"<ns6:response " +
						"xmlns:ns6=\"http://www.vd.ch/unireg/event/get-address-response/1\" " +
						"xmlns=\"http://www.vd.ch/unireg/common/1\" " +
						"xmlns:ns2=\"http://www.vd.ch/unireg/event/request/1\" " +
						"xmlns:ns3=\"http://www.vd.ch/unireg/event/get-address-request/1\" " +
						"xmlns:ns4=\"http://www.vd.ch/unireg/address/1\" " +
						"xmlns:ns5=\"http://www.ech.ch/xmlns/eCH-0010/4\" " +
						"xmlns:ns7=\"http://www.vd.ch/unireg/exception/1\">" +
						"<ns6:addresses>" +
						"<ns4:dateFrom><year>1950</year><month>3</month><day>14</day></ns4:dateFrom>" +
						"<ns4:person>" +
						"<ns5:mrMrs>2</ns5:mrMrs>" +
						"<ns5:firstName>Michel</ns5:firstName>" +
						"<ns5:lastName>Mabelle</ns5:lastName>" +
						"<ns4:salutation>Monsieur</ns4:salutation>" +
						"<ns4:formalGreeting>Monsieur</ns4:formalGreeting>" +
						"</ns4:person>" +
						"<ns4:addressInformation>" +
						"<ns5:street>Les Uttins</ns5:street>" +
						"<ns5:town>Chamblon</ns5:town>" +
						"<ns5:swissZipCode>1436</ns5:swissZipCode>" +
						"<ns5:swissZipCodeId>5876</ns5:swissZipCodeId>" +
						"<ns5:country>CH</ns5:country>" +
						"<ns4:countryName>Suisse</ns4:countryName>" +
						"<ns4:streetId>198539</ns4:streetId>" +
						"<ns4:countryId>8100</ns4:countryId>" +
						"<ns4:tariffZone>SWITZERLAND</ns4:tariffZone>" +
						"</ns4:addressInformation>" +
						"<ns4:formattedAddress>" +
						"<ns4:line1>Monsieur</ns4:line1>" +
						"<ns4:line2>Michel Mabelle</ns4:line2>" +
						"<ns4:line3>Les Uttins</ns4:line3>" +
						"<ns4:line4>1436 Chamblon</ns4:line4>" +
						"</ns4:formattedAddress>" +
						"<ns4:type>RESIDENCE</ns4:type>" +
						"</ns6:addresses>" +
						"</ns6:response>";

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

	private static String requestToString(GetAddressRequest request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createRequest(request), out);
		return out.toString();
	}
}
