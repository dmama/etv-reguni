package ch.vd.uniregctb.evenement.party;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.v1.CreateNonresidentRequest;
import ch.vd.unireg.xml.event.party.nonresident.v1.CreateNonresidentResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v1.Sex;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Classe de test du listener de requêtes de création de non-habitant. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 */
public class PartyCreateNonresidentRequestListenerItTest extends PartyRequestListenerItTest {

	private CreateNonresidentRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = getBean(CreateNonresidentRequestHandler.class, "createNonresidentRequestHandler");
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	String getRequestXSD() {
		return "event/party/create-nonresident-request-1.xsd";
	}

	@Override
	String getResponseXSD() {
		return "event/party/create-nonresident-response-1.xsd";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentRequestUserWithoutAccessRight() throws Exception {
		try {
			test(true, false);
			fail();
		}
		catch (ServiceException e) {
			assertInstanceOf(AccessDeniedExceptionInfo.class, e.getInfo());
			assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
		}

	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresident() throws Exception {
		test(false, false);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentWithoutFirstName() throws Exception {
		test(false, true);
	}

	public void test(boolean sansDroit, boolean sansPrenom) throws Exception {

		final MockSecurityProvider provider = sansDroit ? new MockSecurityProvider() : new MockSecurityProvider(Role.CREATE_NONHAB);
		handler.setSecurityProvider(provider);

		final CreateNonresidentRequest request = createRequest(sansPrenom);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});
		CreateNonresidentResponse res = (CreateNonresidentResponse) parseResponse(getEsbMessage(getOutputQueue()));
		assertNotNull("Le non-habitant devrait être créé", res.getNumber());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testMessageNonConformeNeDoitPasPartirEnDLQ() throws Exception {
		final MockSecurityProvider provider = new MockSecurityProvider(Role.CREATE_NONHAB);
		handler.setSecurityProvider(provider);
		final CreateNonresidentRequest request = createRequest(false);
		final String xmlRequeteSansBaliseLogin = requestToString(request).replaceAll("^(.*)(<[^<]*login>.*</[^<]*login>)(.*)$", "$1$3");
		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				getEsbMessageFactory().setValidator(null); // desactivation du validateur, c'est le but du test d'envoyer un truc pourri
				sendTextMessage(getInputQueue(), xmlRequeteSansBaliseLogin, getOutputQueue());
				return null;
			}
		});
		try {
			parseResponse(getEsbMessage(getOutputQueue()));
		} catch (ServiceException e) {
			assertContains("UnmarshalException", e.getMessage());
			return;
		}
		fail();
	}

	private CreateNonresidentRequest createRequest(boolean sansPrenom) {
		final CreateNonresidentRequest request = new CreateNonresidentRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setCategory(NaturalPersonCategory.SWISS);
		request.setDateOfBirth(new Date(1980,1,1));
		request.setFirstName(sansPrenom ? null : "Pala");
		request.setGender(Sex.MALE);
		request.setLastName("Nabit");
		request.setSocialNumber(7561212121212L);
		return request;
	}



}