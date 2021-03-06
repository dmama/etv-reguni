package ch.vd.unireg.evenement.party;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.v1.CreateNonresidentRequest;
import ch.vd.unireg.xml.event.party.nonresident.v1.CreateNonresidentResponse;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v1.Sex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Classe de test du listener de requêtes de création de non-habitant. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 */
public class PartyCreateNonresidentRequestV1EsbHandlerItTest extends PartyRequestEsbHandlerV1ItTest {

	private CreateNonresidentRequestHandlerV1 handler;

	@NotNull
	@Override
	protected String getRequestHandlerName() {
		return "createNonresidentRequestHandlerV1";
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = getBean(CreateNonresidentRequestHandlerV1.class, "createNonresidentRequestHandlerV1");
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentRequestUserSansDroitAcces() throws Exception {
		testError(false, true, true, false, "L'utilisateur spécifié (xxxxx/22) n'a pas le droit de création de non-habitant sur l'application.");
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresident() throws Exception {
		final long nhId = testRetour(true, true, true, false);
		doInNewTransaction(status -> {
			final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
			assertNotNull(nh);
			assertFalse(nh.isHabitantVD());
			assertNotNull(nh.getNom());
			assertNotNull(nh.getPrenomUsuel());
			assertNotNull(nh.getNumeroAssureSocial());
			return null;
		});
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentSansPrenom() throws Exception {
		final long nhId = testRetour(true, false, true, false);
		doInNewTransaction(status -> {
			final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
			assertNotNull(nh);
			assertFalse(nh.isHabitantVD());
			assertNull(nh.getPrenomUsuel());
			return null;
		});
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentSansAvs13MaisAvecAvs11() throws Exception {
		final long nhId = testRetour(true, true, false, true);
		doInNewTransaction(status -> {
			final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
			assertNotNull(nh);
			assertFalse(nh.isHabitantVD());
//				assertNull(nh.getNumeroAssureSocial());
			assertTrue(nh.getIdentificationsPersonnes() != null || nh.getIdentificationsPersonnes().size() == 1);
			return null;
		});
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentAvecAvs13EtAvecAvs11() throws Exception {
		final long nhId = testRetour(true, true, true, true);
		doInNewTransaction(status -> {
			final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
			assertNotNull(nh);
			assertFalse(nh.isHabitantVD());
			assertNotNull(nh.getNumeroAssureSocial());
			assertTrue(nh.getIdentificationsPersonnes() == null || nh.getIdentificationsPersonnes().size() == 0);
			return null;
		});
	}

	private int testRetour(boolean avecDroit, boolean avecPrenom, boolean avecAvs13, boolean avecAvs11) throws Exception {

		// Envoie le message
		final String businessId = sendMessage(avecDroit, avecPrenom, avecAvs13, avecAvs11);

		final EsbMessage response = getEsbMessage(getOutputQueue());
		assertEquals(businessId, response.getBusinessCorrelationId());

		Thread.sleep(100);

		final CreateNonresidentResponse res = (CreateNonresidentResponse) parseResponse(response);
		assertNotNull("Le non-habitant devrait être créé", res.getNumber());
		return res.getNumber();
	}

	private void testError(boolean avecDroit, boolean avecPrenom, boolean avecAvs13, boolean avecAvs11, String msg) throws Exception {
		// Envoie le message
		final String businessId = sendMessage(avecDroit, avecPrenom, avecAvs13, avecAvs11);

		final EsbMessage error = getEsbBusinessErrorMessage();
		assertEquals(businessId, error.getBusinessId());
		assertEquals(msg, error.getExceptionMessage());
	}

	private String sendMessage(boolean avecDroit, boolean avecPrenom, boolean avecAvs13, boolean avecAvs11) throws Exception {
		final MockSecurityProvider provider = avecDroit ? new MockSecurityProvider(Role.CREATE_NONHAB) : new MockSecurityProvider();
		handler.setSecurityProvider(provider);

		final CreateNonresidentRequest request = createRequest(avecPrenom, avecAvs13, avecAvs11);

		// Envoie le message
		return sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testMessageNonConformeNeDoitPasPartirEnDLQ() throws Exception {
		final MockSecurityProvider provider = new MockSecurityProvider(Role.CREATE_NONHAB);
		handler.setSecurityProvider(provider);
		final CreateNonresidentRequest request = createRequest(true, true, false);
		final String xmlRequeteSansBaliseLogin = requestToString(request).replaceAll("^(.*)(<[^<]*login>.*</[^<]*login>)(.*)$", "$1$3");

		// Envoie le message
		deactivateEsbValidator(); // desactivation du validateur, c'est le but du test d'envoyer un truc pourri
		final String businessId = sendTextMessage(getInputQueue(), xmlRequeteSansBaliseLogin, getOutputQueue());

		final List<EsbMessage> errors = getErrorCollector().waitForIncomingMessages(1, BusinessItTest.JMS_TIMEOUT);
		assertNotNull(errors);
		assertEquals(1, errors.size());

		final EsbMessage msg = errors.get(0);
		assertNotNull(msg);
		assertEquals(businessId, msg.getBusinessId());
		assertEquals(EsbBusinessCode.XML_INVALIDE.getCode(), msg.getHeader(EsbMessage.ERROR_CODE));
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