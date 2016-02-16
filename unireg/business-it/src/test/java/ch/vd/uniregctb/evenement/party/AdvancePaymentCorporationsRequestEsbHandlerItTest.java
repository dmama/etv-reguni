package ch.vd.uniregctb.evenement.party;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.AdvancePaymentPopulationRequest;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.AdvancePaymentPopulationResponse;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.Taxpayer;
import ch.vd.unireg.xml.event.party.v2.Response;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.xml.DataHelper;

/**
 * Classe de test du listener de requêtes de collecte de la population PM des acomptes. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 */
public class AdvancePaymentCorporationsRequestEsbHandlerItTest extends PartyRequestEsbHandlerV2ItTest {

	private AdvancePaymentCorporationsRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		handler = getBean(AdvancePaymentCorporationsRequestHandler.class, "advancePaymentCorporationsRequestHandler");
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	protected List<String> getResponseXSD() {
		return Collections.singletonList("event/party/advance-payment-corporations-response-1.xsd");
	}

	@Override
	protected String getRequestXSD() {
		return "event/party/advance-payment-corporations-request-1.xsd";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testPassant() throws Exception {

		handler.setSecurityProvider(new MockSecurityProvider(Role.VISU_ALL));

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, date(2000, 1, 12), null, "Les joueurs de Belotte du coin");
				addFormeJuridique(e, date(2000, 1, 12), null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalVD(e, date(2000, 1, 12), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalCH(e, date(2000, 1, 12), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addForPrincipal(e, date(2000, 1, 12), MotifFor.DEBUT_EXPLOITATION, MockCommune.Renens);
				addBouclement(e, date(2001, 1, 1), DayMonth.get(12, 31), 12);
				return e.getId();
			}
		});

		final AdvancePaymentPopulationRequest request = new AdvancePaymentPopulationRequest();
		request.setLogin(UserLoginHelper.of("zaimoi", 21));
		request.setReferenceDate(DataHelper.coreToXMLv2(RegDate.get()));

		// Envoi du message
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
			}
		});

		// attente de la réponse
		final Response response = parseResponse(getEsbMessage(getOutputQueue()));
		Assert.assertNotNull(response);
		Assert.assertEquals(AdvancePaymentPopulationResponse.class, response.getClass());

		final AdvancePaymentPopulationResponse populationResponse = (AdvancePaymentPopulationResponse) response;
		Assert.assertEquals(RegDate.get(), DataHelper.xmlToCore(populationResponse.getReferenceDate()));

		final List<Taxpayer> list = populationResponse.getTaxpayer();
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.size());

		final Taxpayer taxpayer = list.get(0);
		Assert.assertNotNull(taxpayer);
		Assert.assertEquals(pmId, taxpayer.getNumber());
		Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), taxpayer.getChTaxSystemType());
		Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), taxpayer.getVdTaxSystemType());
		Assert.assertEquals(date(RegDate.get().year(), 12, 31), DataHelper.xmlToCore(taxpayer.getFutureEndOfBusinessYear()));
		Assert.assertEquals(date(RegDate.get().year() - 1, 12, 31), DataHelper.xmlToCore(taxpayer.getPastEndOfBusinessYear()));
		Assert.assertEquals("Les joueurs de Belotte du coin", taxpayer.getName());
		Assert.assertNotNull(taxpayer.getVdTaxLiability());
		Assert.assertEquals(date(2000, 1, 12), DataHelper.xmlToCore(taxpayer.getVdTaxLiability().getDateFrom()));
		Assert.assertNull(taxpayer.getVdTaxLiability().getDateTo());
		Assert.assertNotNull(taxpayer.getChTaxLiability());
		Assert.assertEquals(date(2000, 1, 12), DataHelper.xmlToCore(taxpayer.getChTaxLiability().getDateFrom()));
		Assert.assertNull(taxpayer.getChTaxLiability().getDateTo());
	}
}
