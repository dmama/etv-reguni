package ch.vd.uniregctb.evenement.party;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.address.v1.AddressRequest;
import ch.vd.unireg.xml.event.party.address.v1.AddressResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
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
public class PartyAddressRequestEsbHandlerItTest extends PartyRequestEsbHandlerV1ItTest {

	private AddressRequestHandler handler;
	protected ProxyServiceCivil serviceCivil;

	@Override
	public void onSetUp() throws Exception {
		handler = getBean(AddressRequestHandler.class, "addressRequestHandler");
		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	protected List<String> getResponseXSD() {
		return Collections.singletonList("event/party/address-response-1.xsd");
	}

	@Override
	protected String getRequestXSD() {
		return "event/party/address-request-1.xsd";
	}
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestUserWithoutAccessRight() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider();
		handler.setSecurityProvider(provider);

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(222);
		request.getTypes().add(AddressType.RESIDENCE);

		// Envoie le message
		final String businessId = doInNewTransaction(new TxCallback<String>() {
			@Override
			public String execute(TransactionStatus status) throws Exception {
				return sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
			}
		});

		final EsbMessage msg = getEsbBusinessErrorMessage();
		assertNotNull(msg);
		assertEquals(businessId, msg.getBusinessId());
		assertEquals(EsbBusinessCode.DROITS_INSUFFISANTS.getCode(), msg.getErrorCode());
		assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", msg.getExceptionMessage());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestOnProtectedFolder() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		provider.setDossiersProteges(222L);
		handler.setSecurityProvider(provider);

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(222);
		request.getTypes().add(AddressType.RESIDENCE);

		// Envoie le message
		final String businessId = doInNewTransaction(new TxCallback<String>() {
			@Override
			public String execute(TransactionStatus status) throws Exception {
				return sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
			}
		});

		final EsbMessage msg = getEsbBusinessErrorMessage();
		assertNotNull(msg);
		assertEquals(businessId, msg.getBusinessId());
		assertEquals(EsbBusinessCode.DROITS_INSUFFISANTS.getCode(), msg.getErrorCode());
		assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture sur le tiers n° 222.", msg.getExceptionMessage());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestOnUnknownParty() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(222);
		request.getTypes().add(AddressType.RESIDENCE);

		// Envoie le message
		final String businessId = doInNewTransaction(new TxCallback<String>() {
			@Override
			public String execute(TransactionStatus status) throws Exception {
				return sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
			}
		});

		final EsbMessage msg = getEsbBusinessErrorMessage();
		assertNotNull(msg);
		assertEquals(businessId, msg.getBusinessId());
		assertEquals(EsbBusinessCode.CTB_INEXISTANT.getCode(), msg.getErrorCode());
		assertEquals("Le tiers n°222 n'existe pas.", msg.getExceptionMessage());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestOK() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

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
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final AddressResponse response = (AddressResponse) parseResponse(getEsbMessage(getOutputQueue()));
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
		assertEquals("Rue des Uttins", info.getStreet());
		assertEquals("Chamblon", info.getTown());
		assertEquals(Long.valueOf(1436), info.getSwissZipCode());
		assertEquals(Integer.valueOf(5876), info.getSwissZipCodeId());
		assertEquals("CH", info.getCountry());
		assertEquals("Suisse", info.getCountryName());
		assertEquals(Integer.valueOf(1142198), info.getStreetId());
		assertEquals(Integer.valueOf(8100), info.getCountryId());
		assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());

		final FormattedAddress formatted = address.getFormattedAddress();
		assertNotNull(formatted);
		assertEquals("Monsieur", formatted.getLine1());
		assertEquals("Michel Mabelle", formatted.getLine2());
		assertEquals("Rue des Uttins", formatted.getLine3());
		assertEquals("1436 Chamblon", formatted.getLine4());
		assertNull(formatted.getLine5());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestOkWithCustomHeader() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

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

		final String headerName = "spiritualFather";
		final String headerValue = "John Lanonne";

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage m = buildTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				m.addHeader(headerName, headerValue);
				validateMessage(m);
				getEsbTemplate().send(m);
				return null;
			}
		});

		final EsbMessage answer = getEsbMessage(getOutputQueue());
		assertNotNull(answer);

		final String foundHeaderValue = answer.getHeader(headerName);
		assertEquals(headerValue, foundHeaderValue);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestKO() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		// créé une personne physique avec une adresse au Kosovo qui ne possède pas de code iso et qui provoque une erreur de validation de l'adresse eCH-0010-4.
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Drago", "Mcic", date(1950, 3, 14), Sexe.MASCULIN);
				addAdresseEtrangere(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 1), null, null, null, MockPays.Kosovo);
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
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		// [SIFISC-5249] On s'assure que le service JMS :
		//  - retourne une réponse
		//  - que cette réponse est une erreur métier dû au code iso manquant du Kosovo
		// msi (24.08.2012), le Kosovo possède dorénavant un code iso, mais comme l'adresse étrangère est vide (à l'exception du pays), la réponse est quand même invalide.
		try {
			parseResponse(getEsbMessage(getOutputQueue()));
		}
		catch (ServiceException e) {
			final ServiceExceptionInfo info = e.getInfo();
			assertInstanceOf(BusinessExceptionInfo.class, info);
			assertEquals(BusinessExceptionCode.INVALID_RESPONSE.name(), ((BusinessExceptionInfo) info).getCode());
			assertContains("Invalid content was found starting with element 'eCH-0010-4:country'", info.getMessage()); // en erreur parce qu'il manque soit une ligne d'adresse, la rue ou la localité.
		}
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestSansAddressInfoPaysInconnu() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		final long noInd = 12345L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1976, 5, 12), "Dffru", "Rhoo", true);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE,null,"toto street","tttt",MockPays.PaysInconnu,date(1950, 3, 1),null);
			}
		});


		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(id.intValue());
		request.getTypes().add(AddressType.MAIL);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});
		try {
			AddressResponse adressResponse = (AddressResponse) parseResponse(getEsbMessage(getOutputQueue()));
			Address adress =adressResponse.getAddresses().get(0);
			assertNull(adress.getAddressInformation());
		}
		catch (ServiceException e) {
		fail("la réponse ne respecte pas la xsd eCH-0010");
		}

	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testAddressRequestSansAddressInfoPasDeVille() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		final long noInd = 12345L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1976, 5, 12), "Dffru", "Rhoo", true);
				addAdresse(ind, TypeAdresseCivil.COURRIER,null,"toto street",null,MockPays.RoyaumeUni,date(1950, 3, 1),null);
			}
		});


		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		final AddressRequest request = new AddressRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(id.intValue());
		request.getTypes().add(AddressType.MAIL);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});
		try {
			AddressResponse adressResponse = (AddressResponse) parseResponse(getEsbMessage(getOutputQueue()));
			Address adress =adressResponse.getAddresses().get(0);
			assertNull(adress.getAddressInformation());
		}
		catch (ServiceException e) {
			fail("la réponse ne respecte pas la xsd eCH-0010");
		}

	}


}
