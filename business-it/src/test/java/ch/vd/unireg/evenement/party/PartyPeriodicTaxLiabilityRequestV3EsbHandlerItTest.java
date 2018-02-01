package ch.vd.unireg.evenement.party;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.taxliab.periodic.v3.PeriodicTaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.v3.TaxLiabilityResponse;
import ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Classe de test du listener de résolution de l'assujettissement  V3
 *
 * @author Baba NGOM <baba-issa.ngom@vd.ch>
 */
public class PartyPeriodicTaxLiabilityRequestV3EsbHandlerItTest extends PartyRequestEsbHandlerV1ItTest {

	private PeriodicTaxLiabilityRequestHandlerV3 handlerV3;

	@Override
	public void onSetUp() throws Exception {
		handlerV3 = getBean(PeriodicTaxLiabilityRequestHandlerV3.class, "periodicTaxLiabilityRequestHandlerV3");
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		handlerV3.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	protected String getRequestXSD() {
		return "event/party/periodic-taxliab-request-3.xsd";
	}

	@Override
	protected List<String> getResponseXSD() {
		return Collections.singletonList("event/party/taxliab-response-3.xsd");
	}


	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestKOPeriodeDansLeFutur() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handlerV3.setSecurityProvider(provider);

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Jacques", "Ramaldadji", date(1965, 3, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, MockCommune.Vevey);
				return pp.getNumero();
			}
		});

		final PeriodicTaxLiabilityRequest request = new PeriodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		int periodeFuture = RegDate.get().year() + 1;
		request.setFiscalPeriod(periodeFuture);
		request.setSearchCommonHouseHolds(false);
		request.setSearchParents(false);

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		// on s'assure que la réponse est bien positive
		final TaxLiabilityResponse response = (TaxLiabilityResponse) parseResponse(message);
		assertNotNull(response);
		assertNull(response.getPartyNumber());
		assertNotNull(response.getFailure());
		assertNotNull(response.getFailure().getDateOrPeriodeInFuture());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testRequestKOAssujetissementNonConforme() throws Exception {

		final MockSecurityProvider provider = new MockSecurityProvider(Role.VISU_ALL);
		handlerV3.setSecurityProvider(provider);

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Jacques", "Ramaldadji", date(1965, 3, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, MockCommune.Vevey, ModeImposition.MIXTE_137_1);
				return pp.getNumero();
			}
		});

		final PeriodicTaxLiabilityRequest request = new PeriodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		int periode = RegDate.get().year();
		request.setFiscalPeriod(periode);
		request.setSearchCommonHouseHolds(false);
		request.setSearchParents(false);
		request.getIndividualTaxLiabilityToReject().add(IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_1);
		request.getIndividualTaxLiabilityToReject().add(IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_2);

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		// on s'assure que la réponse est bien positive
		final TaxLiabilityResponse response = (TaxLiabilityResponse) parseResponse(message);
		assertNotNull(response);
		assertNull(response.getPartyNumber());
		assertNotNull(response.getFailure());
		assertNotNull(response.getFailure().getImproperTaxliability());
	}


}
