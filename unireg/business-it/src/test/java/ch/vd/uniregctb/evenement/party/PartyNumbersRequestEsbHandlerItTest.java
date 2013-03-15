package ch.vd.uniregctb.evenement.party;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersRequest;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Classe de test du listener de requêtes de résolution d'adresses. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PartyNumbersRequestEsbHandlerItTest extends PartyRequestEsbHandlerItTest {

	private NumbersRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		handler = getBean(NumbersRequestHandler.class, "numberRequestHandler");
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	String getRequestXSD() {
		return "event/party/numbers-request-1.xsd";
	}

	@Override
	String getResponseXSD() {
		return "event/party/numbers-response-1.xsd";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testNumbersRequestUserWithoutAccessRight() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider();
		handler.setSecurityProvider(provider);

		final NumbersRequest request = new NumbersRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setIncludeCancelled(false);
		request.getTypes().add(PartyType.NATURAL_PERSON);

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
	public void testNumbersRequestOK() throws Exception {

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

		final NumbersRequest request = new NumbersRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setIncludeCancelled(false);
		request.getTypes().add(PartyType.NATURAL_PERSON);

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

		final NumbersResponse response = (NumbersResponse) parseResponse(message);
		assertNotNull(response);
		assertEquals(1, response.getIdsCount());

		final List<Integer> ids = parseIds(message);
		assertNotNull(ids);
		assertEquals(1, ids.size());
		assertEquals(id.intValue(), ids.get(0).intValue());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testNumbersRequestOkWithCustomHeader() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handler.setSecurityProvider(provider);

		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
				addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, MockRue.Chamblon.RueDesUttins);
				return pp.getNumero();
			}
		});

		final NumbersRequest request = new NumbersRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setIncludeCancelled(false);
		request.getTypes().add(PartyType.NATURAL_PERSON);

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

	private static List<Integer> parseIds(EsbMessage message) throws Exception {
		final InputStream idsAsStream = message.getAttachmentAsStream("ids");
		if (idsAsStream == null) {
			return Collections.emptyList();
		}

		final List<Integer> ids = new ArrayList<>();
		try (Scanner scanner = new Scanner(idsAsStream, "UTF-8")) {
			while (scanner.hasNext()) {
				ids.add(Integer.parseInt(scanner.next()));
			}
		}
		return ids;
	}

}
