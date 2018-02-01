package ch.vd.uniregctb.evenement.party;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.xml.event.party.party.v5.PartyRequest;
import ch.vd.unireg.xml.event.party.party.v5.PartyResponse;
import ch.vd.unireg.xml.event.party.v2.Response;
import ch.vd.unireg.xml.party.person.v5.NaturalPerson;
import ch.vd.unireg.xml.party.person.v5.Sex;
import ch.vd.unireg.xml.party.taxpayer.v5.FamilyStatus;
import ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriod;
import ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriodType;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GetPartyRequestV5EsbHandlerItTest extends PartyRequestEsbHandlerV2ItTest {

	private PartyRequestHandlerV5 handler;
	private ProxyServiceCivil serviceCivil;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = getBean(PartyRequestHandlerV5.class, "partyRequestHandlerV5");
		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Override
	protected String getRequestXSD() {
		return "event/party/party-request-5.xsd";
	}

	@Override
	protected List<String> getResponseXSD() {
		return Arrays.asList("event/party/party-response-5.xsd",
		                     "party/unireg-party-administrativeauthority-5.xsd",
		                     "party/unireg-party-corporation-5.xsd",
		                     "party/unireg-party-othercommunity-3.xsd",
		                     "party/unireg-party-debtor-5.xsd",
		                     "party/unireg-party-establishment-2.xsd",
		                     "party/unireg-party-person-5.xsd");
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testSimpleCaseWithSomeParts() throws Exception {

		final long noIndividu = 3278347L;
		final RegDate dateNaissance = date(1985, 6, 12);
		final RegDate majorite = dateNaissance.addYears(18);

		handler.setSecurityProvider(new MockSecurityProvider(Role.VISU_ALL));

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Malapalud", "Alfred", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, majorite, MotifFor.MAJORITE, MockCommune.Echallens);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(2006, 5, 12), date(2006, 9, 21), false);

				return pp.getNumero();
			}
		});

		final PartyRequest request = new PartyRequest();
		request.setLogin(UserLoginHelper.of("zaimoi", 22));
		request.setPartyNumber((int) ppId);
		request.getParts().add(PartyPart.FAMILY_STATUSES);
		request.getParts().add(PartyPart.WITHHOLDING_TAXATION_PERIODS);

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final Response response = parseResponse(getEsbMessage(getOutputQueue()));
		assertNotNull(response);
		assertEquals(PartyResponse.class, response.getClass());

		final PartyResponse partyResponse = (PartyResponse) response;
		assertEquals((int) ppId, partyResponse.getPartyNumber());
		assertNotNull(partyResponse.getParty());
		assertEquals(NaturalPerson.class, partyResponse.getParty().getClass());

		final NaturalPerson pp = (NaturalPerson) partyResponse.getParty();
		assertEquals("Malapalud", pp.getOfficialName());
		assertEquals("Alfred", pp.getFirstName());
		assertEquals(Sex.MALE, pp.getSex());
		assertEquals(dateNaissance, DataHelper.xmlToCore(pp.getDateOfBirth()));

		final List<FamilyStatus> familyStatuses = pp.getFamilyStatuses();
		assertNotNull(familyStatuses);
		assertEquals(1, familyStatuses.size());
		{
			final FamilyStatus status = familyStatuses.get(0);
			assertNotNull(status);
			assertEquals(MaritalStatus.SINGLE, status.getMaritalStatus());
			assertEquals(dateNaissance, DataHelper.xmlToCore(status.getDateFrom()));
			assertNull(status.getDateTo());
		}

		final List<WithholdingTaxationPeriod> piis = pp.getWithholdingTaxationPeriods();
		assertNotNull(piis);
		assertEquals(1, piis.size());
		{
			final WithholdingTaxationPeriod wtp = piis.get(0);
			assertNotNull(wtp);
			assertEquals(date(2006, 1, 1), DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(date(2006, 12, 31), DataHelper.xmlToCore(wtp.getDateTo()));
			assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), wtp.getTaxationAuthorityFSOId());
		}

		assertEmpty(pp.getMailAddresses());
		assertEmpty(pp.getDebtProsecutionAddresses());
		assertEmpty(pp.getResidenceAddresses());
		assertEmpty(pp.getRepresentationAddresses());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testUnknownParty() throws Exception {

		handler.setSecurityProvider(new MockSecurityProvider(Role.VISU_ALL));

		final PartyRequest request = new PartyRequest();
		request.setLogin(UserLoginHelper.of("zaimoi", 22));
		request.setPartyNumber(42);
		request.getParts().add(PartyPart.FAMILY_STATUSES);
		request.getParts().add(PartyPart.WITHHOLDING_TAXATION_PERIODS);

		// Envoie le message
		final String businessId = sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final List<EsbMessage> errors = getErrorCollector().waitForIncomingMessages(1, BusinessItTest.JMS_TIMEOUT);
		assertNotNull(errors);
		assertEquals(1, errors.size());

		final EsbMessage error = errors.get(0);
		assertNotNull(error);
		assertEquals(businessId, error.getBusinessId());
		assertEquals(EsbBusinessCode.CTB_INEXISTANT.getCode(), error.getHeader(EsbMessage.ERROR_CODE));
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testVisualisationLimitee() throws Exception {

		final long noIndividu = 3278347L;
		final RegDate dateNaissance = date(1985, 6, 12);
		final RegDate majorite = dateNaissance.addYears(18);

		handler.setSecurityProvider(new MockSecurityProvider(Role.VISU_LIMITE));

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Malapalud", "Alfred", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, majorite, MotifFor.MAJORITE, MockCommune.Echallens);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(2006, 5, 12), date(2006, 9, 21), false);

				return pp.getNumero();
			}
		});

		final PartyRequest request = new PartyRequest();
		request.setLogin(UserLoginHelper.of("zaimoi", 22));
		request.setPartyNumber((int) ppId);
		request.getParts().add(PartyPart.FAMILY_STATUSES);
		request.getParts().add(PartyPart.WITHHOLDING_TAXATION_PERIODS);

		// Envoie le message
		final String businessId = sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final List<EsbMessage> errors = getErrorCollector().waitForIncomingMessages(1, BusinessItTest.JMS_TIMEOUT);
		assertNotNull(errors);
		assertEquals(1, errors.size());

		final EsbMessage error = errors.get(0);
		assertNotNull(error);
		assertEquals(businessId, error.getBusinessId());
		assertEquals(EsbBusinessCode.DROITS_INSUFFISANTS.getCode(), error.getHeader(EsbMessage.ERROR_CODE));
	}

}
