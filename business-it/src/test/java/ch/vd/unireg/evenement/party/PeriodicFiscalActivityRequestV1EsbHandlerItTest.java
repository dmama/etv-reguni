package ch.vd.unireg.evenement.party;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.fiscact.periodic.v1.PeriodicFiscalActivityRequest;
import ch.vd.unireg.xml.event.party.fiscact.v1.FiscalActivityResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PeriodicFiscalActivityRequestV1EsbHandlerItTest extends PartyRequestEsbHandlerV1ItTest {

	private PeriodicFiscalActivityRequestHandlerV1 handler;

	@NotNull
	@Override
	protected String getRequestHandlerName() {
		return "periodicFiscalActivityRequestHandlerV1";
	}

	@Override
	public void onSetUp() throws Exception {
		handler = getBean(PeriodicFiscalActivityRequestHandlerV1.class, "periodicFiscalActivityRequestHandlerV1");
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestUserWithoutAccessRight() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider();
		handler.setSecurityProvider(provider);

		final PeriodicFiscalActivityRequest request = new PeriodicFiscalActivityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setYear(2009);
		request.setPartyNumber(42);

		// Envoie le message
		final String businessId = sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage msg = getEsbBusinessErrorMessage();
		assertNotNull(msg);
		assertEquals(businessId, msg.getBusinessId());
		assertEquals(EsbBusinessCode.DROITS_INSUFFISANTS.getCode(), msg.getErrorCode());
		assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture sur les fors fiscaux.", msg.getExceptionMessage());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestOK() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
			addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, MockRue.Chamblon.RueDesUttins);
			addForPrincipal(pp, date(1980, 6, 1), MotifFor.MAJORITE, MockCommune.Aigle);
			return pp.getNumero();
		});

		final PeriodicFiscalActivityRequest request = new PeriodicFiscalActivityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setYear(1992);
		request.setPartyNumber(id.intValue());

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final FiscalActivityResponse response = (FiscalActivityResponse) parseResponse(message);
		assertNotNull(response);
		assertTrue(response.isActive());
		assertEquals("Le contribuable a un for vaudois ouvert sur la période demandée.", response.getMessage());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestOkWithCustomHeader() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
			addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, MockRue.Chamblon.RueDesUttins);
			addForPrincipal(pp, date(1980, 6, 1), MotifFor.MAJORITE, MockCommune.Aigle);
			return pp.getNumero();
		});

		final PeriodicFiscalActivityRequest request = new PeriodicFiscalActivityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setYear(1992);
		request.setPartyNumber(id.intValue());

		final String headerName = "spiritualFather";
		final String headerValue = "John Lanonne";

		// Envoie le message
		final EsbMessage m = buildTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
		m.addHeader(headerName, headerValue);
		validateMessage(m);
		EvenementHelper.sendMessage(getEsbTemplate(), m, transactionManager);

		final EsbMessage answer = getEsbMessage(getOutputQueue());
		assertNotNull(answer);

		final String foundHeaderValue = answer.getHeader(headerName);
		assertEquals(headerValue, foundHeaderValue);
	}
}
