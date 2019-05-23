package ch.vd.unireg.evenement.party;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.party.v3.PartyRequest;
import ch.vd.unireg.xml.event.party.party.v3.PartyResponse;
import ch.vd.unireg.xml.event.party.v1.Response;
import ch.vd.unireg.xml.party.person.v3.NaturalPerson;
import ch.vd.unireg.xml.party.person.v3.Sex;
import ch.vd.unireg.xml.party.taxpayer.v3.FamilyStatus;
import ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v2.WithholdingTaxationPeriod;
import ch.vd.unireg.xml.party.taxresidence.v2.WithholdingTaxationPeriodType;
import ch.vd.unireg.xml.party.v3.PartyPart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GetPartyRequestV3EsbHandlerItTest extends PartyRequestEsbHandlerV1ItTest {

	private PartyRequestHandlerV3 handler;
	private ProxyServiceCivil serviceCivil;

	@NotNull
	@Override
	protected String getRequestHandlerName() {
		return "partyRequestHandlerV3";
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = getBean(PartyRequestHandlerV3.class, "partyRequestHandlerV3");
		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testSimpleCaseWithSomeParts() throws Exception {

		final long noIndividu = 3278347L;
		final RegDate dateNaissance = date(1985, 6, 12);
		final RegDate majorite = dateNaissance.addYears(18);

		handler.setSecurityProvider(new MockSecurityProvider(Role.VISU_ALL));

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Malapalud", "Alfred", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, majorite, MotifFor.MAJORITE, MockCommune.Echallens);

			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			addRapportPrestationImposable(dpi, pp, date(2006, 5, 12), date(2006, 9, 21), false);
			return pp.getNumero();
		});

		final PartyRequest request = new PartyRequest();
		request.setLogin(new UserLogin("zaimoi", 22));
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
		request.setLogin(new UserLogin("zaimoi", 22));
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
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Malapalud", "Alfred", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, majorite, MotifFor.MAJORITE, MockCommune.Echallens);

			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			addRapportPrestationImposable(dpi, pp, date(2006, 5, 12), date(2006, 9, 21), false);
			return pp.getNumero();
		});

		final PartyRequest request = new PartyRequest();
		request.setLogin(new UserLogin("zaimoi", 22));
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
