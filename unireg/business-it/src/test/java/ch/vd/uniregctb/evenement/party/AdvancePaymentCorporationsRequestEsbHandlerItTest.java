package ch.vd.uniregctb.evenement.party;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.AdvancePaymentPopulationRequest;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.AdvancePaymentPopulationResponse;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.AttachmentDescriptor;
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
				addRaisonSociale(e, date(2000, 1, 12), null, "Les joueurs de Belotte du préau");
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
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		// attente de la réponse
		final EsbMessage esbMessage = getEsbMessage(getOutputQueue());
		Assert.assertNotNull(esbMessage);
		final Response response = parseResponse(esbMessage);
		Assert.assertNotNull(response);
		Assert.assertEquals(AdvancePaymentPopulationResponse.class, response.getClass());

		final AdvancePaymentPopulationResponse populationResponse = (AdvancePaymentPopulationResponse) response;
		Assert.assertEquals(RegDate.get(), DataHelper.xmlToCore(populationResponse.getReferenceDate()));

		final AttachmentDescriptor population = populationResponse.getPopulation();
		Assert.assertNotNull(population);
		Assert.assertEquals(1, population.getLines());
		final String populationFilename = population.getAttachmentName();
		Assert.assertNotNull(populationFilename);
		final String encoding = population.getCharacterEncoding();
		Assert.assertNotNull(encoding);

		try (InputStream is = esbMessage.getAttachmentAsStream(populationFilename);
		     Reader r = new InputStreamReader(is, encoding);
		     BufferedReader br = new BufferedReader(r)) {

			// il doit y avoir deux lignes dans ce fichier : la ligne des colonnes et la ligne des données de l'entreprise

			final String colonnes = br.readLine();
			Assert.assertEquals("NO_CTB;RAISON_SOCIALE;DATE_BOUCLEMENT_FUTUR;DATE_BOUCLEMENT_PRECEDENT;DEBUT_ICC;FIN_ICC;DEBUT_IFD;FIN_IFD;RF_VD;RF_CH", colonnes);

			final String data = br.readLine();
			Assert.assertEquals(String.format("%d;%s;%s;%s;%s;%s;%s;%s;%s;%s",
			                                  pmId,
			                                  "Les joueurs de Belotte du préau",
			                                  RegDateHelper.dateToDisplayString(date(RegDate.get().year(), 12, 31)),
			                                  RegDateHelper.dateToDisplayString(date(RegDate.get().year() - 1, 12, 31)),
			                                  "12.01.2000",
			                                  StringUtils.EMPTY,
			                                  "12.01.2000",
			                                  StringUtils.EMPTY,
			                                  MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(),
			                                  MockTypeRegimeFiscal.ORDINAIRE_APM.getCode()),
			                    data);

			Assert.assertNull(br.readLine());
		}
	}
}
