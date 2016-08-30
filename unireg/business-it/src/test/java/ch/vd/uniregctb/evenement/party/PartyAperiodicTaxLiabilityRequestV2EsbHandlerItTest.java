package ch.vd.uniregctb.evenement.party;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v2.AperiodicTaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.v2.TaxLiabilityResponse;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * [SIFISC-7731] Classe de test du listener de résolution de l'assujettissement apériodique des contribuables. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PartyAperiodicTaxLiabilityRequestV2EsbHandlerItTest extends PartyRequestEsbHandlerV1ItTest {

	private AperiodicTaxLiabilityRequestHandlerV2 handler;

	@Override
	public void onSetUp() throws Exception {
		handler = getBean(AperiodicTaxLiabilityRequestHandlerV2.class, "aperiodicTaxLiabilityRequestHandlerV2");
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	protected String getRequestXSD() {
		return "event/party/aperiodic-taxliab-request-2.xsd";
	}

	@Override
	protected List<String> getResponseXSD() {
		return Arrays.asList("event/party/taxliab-response-2.xsd");
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestUserWithoutAccessRight() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider();
		handler.setSecurityProvider(provider);

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setDate(new Date(2000, 1, 1));
		request.setPartyNumber(12345678);

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

	@Test//(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestOK() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Jacques", "Ramaldadji", date(1965, 3, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, MockCommune.Vevey);
				return pp.getNumero();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setSearchCommonHouseHolds(false);
		request.setSearchParents(false);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		// on s'assure que la réponse est bien positive
		final TaxLiabilityResponse response = (TaxLiabilityResponse) parseResponse(message);
		assertNotNull(response);
		assertEquals(idPP.intValue(), response.getPartyNumber().intValue());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestNotOk() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Jacques", "Ramaldadji", date(1965, 3, 12), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setSearchCommonHouseHolds(false);
		request.setSearchParents(false);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		// on s'assure que la réponse est bien positive
		final TaxLiabilityResponse response = (TaxLiabilityResponse) parseResponse(message);
		assertNotNull(response);
		assertNull(response.getPartyNumber());
		assertNotNull(response.getFailure());
		assertNotNull(response.getFailure().getNoTaxLiability());
	}


}
