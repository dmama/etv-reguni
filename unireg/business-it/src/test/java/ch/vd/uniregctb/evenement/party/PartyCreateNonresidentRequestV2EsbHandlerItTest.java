package ch.vd.uniregctb.evenement.party;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.v2.CreateNonresidentRequest;
import ch.vd.unireg.xml.event.party.nonresident.v2.CreateNonresidentResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v2.Sex;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Classe de test du listener de requêtes de création de non-habitant. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 */
public class PartyCreateNonresidentRequestV2EsbHandlerItTest extends PartyRequestEsbHandlerItTest {

	private CreateNonresidentRequestHandlerV2 handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = getBean(CreateNonresidentRequestHandlerV2.class, "createNonresidentRequestHandlerV2");
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	protected String getRequestXSD() {
		return "event/party/create-nonresident-request-2.xsd";
	}

	@Override
	protected String getResponseXSD() {
		return "event/party/create-nonresident-response-2.xsd";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentRequestUserSansDroitAcces() throws Exception {
		try {
			test(false, true, true, false);
			fail();
		}
		catch (ServiceException e) {
			assertInstanceOf(AccessDeniedExceptionInfo.class, e.getInfo());
			assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas le droit de création de non-habitant sur l'application.", e.getMessage());
		}

	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresident() throws Exception {
		final long nhId = test(true, true, true, false);
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
				assertNotNull(nh);
				assertFalse(nh.isHabitantVD());
				assertNotNull(nh.getNom());
				assertNotNull(nh.getPrenom());
				assertNotNull(nh.getNumeroAssureSocial());
			}
		});
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentSansPrenom() throws Exception {
		final long nhId = test(true, false, true, false);
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
				assertNotNull(nh);
				assertFalse(nh.isHabitantVD());
				assertNull(nh.getPrenom());
			}
		});
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentSansAvs13MaisAvecAvs11() throws Exception {
		final long nhId = test(true, true, false, true);
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
				assertNotNull(nh);
				assertFalse(nh.isHabitantVD());
//				assertNull(nh.getNumeroAssureSocial());
				assertTrue(nh.getIdentificationsPersonnes() != null || nh.getIdentificationsPersonnes().size() == 1);
			}
		});
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentAvecAvs13EtAvecAvs11() throws Exception {
		final long nhId = test(true, true, true, true);
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
				assertNotNull(nh);
				assertFalse(nh.isHabitantVD());
				assertNotNull(nh.getNumeroAssureSocial());
				assertTrue(nh.getIdentificationsPersonnes() == null || nh.getIdentificationsPersonnes().size() == 0);
			}
		});
	}

	public int test(boolean avecDroit, boolean avecPrenom, boolean avecAvs13, boolean avecAvs11) throws Exception {

		final MockSecurityProvider provider = avecDroit ? new MockSecurityProvider(Role.CREATE_NONHAB) : new MockSecurityProvider();
		handler.setSecurityProvider(provider);

		final CreateNonresidentRequest request = createRequest(avecPrenom, avecAvs13, avecAvs11);

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
		return res.getNumber();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testMessageNonConformeNeDoitPasPartirEnDLQ() throws Exception {
		final MockSecurityProvider provider = new MockSecurityProvider(Role.CREATE_NONHAB);
		handler.setSecurityProvider(provider);
		final CreateNonresidentRequest request = createRequest(true, true, false);
		final String xmlRequeteSansBaliseLogin = requestToString(request).replaceAll("^(.*)(<[^<]*login>.*</[^<]*login>)(.*)$", "$1$3");
		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				deactivateEsbValidator(); // desactivation du validateur, c'est le but du test d'envoyer un truc pourri
				sendTextMessage(getInputQueue(), xmlRequeteSansBaliseLogin, getOutputQueue());
				return null;
			}
		});
		try {
			parseResponse(getEsbMessage(getOutputQueue()));
			fail();
		}
		catch (ServiceException e) {
			assertContains("UnmarshalException", e.getMessage());
		}
	}

	private CreateNonresidentRequest createRequest(boolean avecPrenom, boolean avecAvs13, boolean avecAvs11) {
		final CreateNonresidentRequest request = new CreateNonresidentRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setCategory(NaturalPersonCategory.SWISS);
		request.setDateOfBirth(new Date(1980,1,1));
		request.setFirstName(avecPrenom ? "Pala" : null);
		request.setGender(Sex.MALE);
		request.setLastName("Nabit");
		request.setSocialNumber(avecAvs13? 7561212121212L : null);
		request.setOldSocialNumber(avecAvs11 ? 12345678901L : null);
		return request;
	}
}