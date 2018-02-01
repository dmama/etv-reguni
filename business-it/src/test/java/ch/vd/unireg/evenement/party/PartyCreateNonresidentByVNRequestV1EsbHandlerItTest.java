package ch.vd.unireg.evenement.party;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.common.v2.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.vn.v1.CreateNonresidentByVNRequest;
import ch.vd.unireg.xml.event.party.nonresident.vn.v1.CreateNonresidentByVNResponse;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener de requêtes de création de non-habitant d'après un simple numéro AVS. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
public class PartyCreateNonresidentByVNRequestV1EsbHandlerItTest extends PartyRequestEsbHandlerV2ItTest {

	private CreateNonresidentByVNRequestHandlerV1 handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = getBean(CreateNonresidentByVNRequestHandlerV1.class, "createNonresidentByVNRequestHandlerV1");
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	protected String getRequestXSD() {
		return "event/party/create-nonresident-byvn-request-1.xsd";
	}

	@Override
	protected List<String> getResponseXSD() {
		return Collections.singletonList("event/party/create-nonresident-byvn-response-1.xsd");
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresidentRequestUserSansDroitAcces() throws Exception {
		testError(false, "L'utilisateur spécifié (xxxxx/22) n'a pas le droit de création de non-habitant sur l'application.");
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testCreateNonresident() throws Exception {
		final long nhId = testRetour(true);
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(nhId);
				assertNotNull(nh);
				assertFalse(nh.isHabitantVD());
				assertNotNull(nh.getNom());
				assertNotNull(nh.getPrenomUsuel());
				assertNotNull(nh.getNumeroAssureSocial());
			}
		});
	}

	private int testRetour(boolean avecDroit) throws Exception {

		// Envoie le message
		final String businessId = sendMessage(avecDroit);

		final EsbMessage response = getEsbMessage(getOutputQueue());
		assertEquals(businessId, response.getBusinessCorrelationId());

		Thread.sleep(100);

		final CreateNonresidentByVNResponse res = (CreateNonresidentByVNResponse) parseResponse(response);
		assertNotNull("Le non-habitant devrait être créé", res.getNumber());
		return res.getNumber();
	}

	private void testError(boolean avecDroit, String msg) throws Exception {
		// Envoie le message
		final String businessId = sendMessage(avecDroit);

		final EsbMessage error = getEsbBusinessErrorMessage();
		assertEquals(businessId, error.getBusinessId());
		assertEquals(msg, error.getExceptionMessage());
	}

	private String sendMessage(boolean avecDroit) throws Exception {
		final MockSecurityProvider provider = avecDroit ? new MockSecurityProvider(Role.CREATE_NONHAB) : new MockSecurityProvider();
		handler.setSecurityProvider(provider);

		final CreateNonresidentByVNRequest request = createRequest();

		// Envoie le message
		return sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testMessageNonConformeNeDoitPasPartirEnDLQ() throws Exception {
		final MockSecurityProvider provider = new MockSecurityProvider(Role.CREATE_NONHAB);
		handler.setSecurityProvider(provider);
		final CreateNonresidentByVNRequest request = createRequest();
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

	private CreateNonresidentByVNRequest createRequest() {
		final CreateNonresidentByVNRequest request = new CreateNonresidentByVNRequest();
		final UserLogin login = new UserLogin("xxxxx/22");
		request.setLogin(login);
		request.setSocialNumber(7568409992270L);
		return request;
	}
}