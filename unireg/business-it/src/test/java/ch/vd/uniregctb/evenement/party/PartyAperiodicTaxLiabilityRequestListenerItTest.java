package ch.vd.uniregctb.evenement.party;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v1.AperiodicTaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v1.AperiodicTaxLiabilityResponse;
import ch.vd.unireg.xml.event.party.taxliab.common.v1.ResponseType;
import ch.vd.unireg.xml.event.party.taxliab.common.v1.Scope;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * [SIFISC-7731] Classe de test du listener de résolution de l'assujettissement apériodique des contribuables. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PartyAperiodicTaxLiabilityRequestListenerItTest extends PartyRequestListenerItTest {

	private AperiodicTaxLiabilityRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		handler = getBean(AperiodicTaxLiabilityRequestHandler.class, "aperiodicTaxLiabilityRequestHandler");
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	String getRequestXSD() {
		return "event/party/aperiodic-taxliab-request-1.xsd";
	}

	@Override
	String getResponseXSD() {
		return "event/party/aperiodic-taxliab-response-1.xsd";
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
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		try {
			parseResponse(getEsbMessage(getOutputQueue()));
			fail();
		}
		catch (ServiceException e) {
			assertInstanceOf(AccessDeniedExceptionInfo.class, e.getInfo());
			assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
		}
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
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
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

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
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) parseResponse(message);
		assertNotNull(response);
		assertEquals(idPP.intValue(), response.getPartyNumber().intValue());
		assertEquals(ResponseType.OK_TAXPAYER_FOUND, response.getType());

		final TaxResidence mainTaxResidence = response.getMainTaxResidence();
		assertNotNull(mainTaxResidence);
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, mainTaxResidence.getTaxationAuthorityType());
		assertEquals(MockCommune.Vevey.getNoOFS(), mainTaxResidence.getTaxationAuthorityFSOId());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestOkWithCustomHeader() throws Exception {

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
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final String headerName = "spiritualFather";
		final String headerValue = "John Lanonne";

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage m = buildTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				m.addHeader(headerName, headerValue);
				getEsbTemplate().send(m);
				return null;
			}
		});

		final EsbMessage answer = getEsbMessage(getOutputQueue());
		assertNotNull(answer);

		final String foundHeaderValue = answer.getHeader(headerName);
		assertEquals(headerValue, foundHeaderValue);
	}
}
