package ch.vd.unireg.evenement.party;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.LienAssociesEtSNC;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.party.v4.PartyRequest;
import ch.vd.unireg.xml.event.party.party.v4.PartyResponse;
import ch.vd.unireg.xml.event.party.v1.Response;
import ch.vd.unireg.xml.party.person.v4.NaturalPerson;
import ch.vd.unireg.xml.party.v4.PartyPart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ADDIGetPartyRequestV4EsbHandlerItTest extends PartyRequestEsbHandlerV1ItTest {

	private PartyRequestHandlerV4 handler;
	private ProxyServiceCivil serviceCivil;

	@NotNull
	@Override
	protected String getRequestHandlerName() {
		return "partyRequestHandlerV4";
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = getBean(PartyRequestHandlerV4.class, "partyRequestHandlerV4");
		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
		super.onTearDown();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testSimpleCaseWithSomeParts() throws Exception {

		final long noIndividu = 10110132L;
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

			// ajout des rapports entre tiers
			final RegDate dateDebut = date(2008, 5, 1);
			final Entreprise snc = addEntrepriseInconnueAuCivil();
			addRaisonSociale(snc, dateDebut, null, "Paris saint germain football");
			addFormeJuridique(snc, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(snc, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(snc, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);

			final LienAssociesEtSNC rapport = new LienAssociesEtSNC(RegDate.get(2002, 2, 1), null, pp, snc);
			tiersDAO.save(rapport);
			return pp.getNumero();
		});

		final PartyRequest request = new PartyRequest();
		request.setLogin(new UserLogin("ZAIZZT", 22));
		request.setPartyNumber((int) ppId);
		request.getParts().add(PartyPart.RELATIONS_BETWEEN_PARTIES);

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final Response response = parseResponse(getEsbMessage(getOutputQueue()));
		assertNotNull(response);
		assertEquals(PartyResponse.class, response.getClass());

		final PartyResponse partyResponse = (PartyResponse) response;
		assertEquals((int) ppId, partyResponse.getPartyNumber());
		assertNotNull(partyResponse.getParty());
		assertEquals(NaturalPerson.class, partyResponse.getParty().getClass());

		doInNewTransactionAndSession(status -> {
			final NaturalPerson pp = (NaturalPerson) partyResponse.getParty();
			final PersonnePhysique ppDB = (PersonnePhysique) tiersDAO.get(ppId);
			//on verifie qu'il existe bel et bien un rapport de type SNC pour ce contribuable en BDD.
			assertEquals(ppDB.getNumero().intValue(), pp.getNumber());
			assertEquals(ppDB.getRapportsObjet().size(), 1);
			final RapportEntreTiers rapportEntreTiers = ppDB.getRapportsObjet().iterator().next();
			assertEquals(rapportEntreTiers.getType(), TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC);
			// on vérifie qu'aucun rapport de type SNC n'est remontée par le get party V4.
			assertEquals(pp.getRelationsBetweenParties().size(), 0);
			return null;
		});
	}


}
