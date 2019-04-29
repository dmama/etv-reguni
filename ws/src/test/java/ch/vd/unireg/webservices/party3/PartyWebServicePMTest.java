package ch.vd.unireg.webservices.party3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.WebserviceTest;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.OrganisationMailAddressInfo;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class PartyWebServicePMTest extends WebserviceTest {

	private PartyWebService service;
	private UserLogin login;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(PartyWebService.class, "partyService3Impl");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		login = new UserLogin("iamtestuser", 22);
		serviceCivil.setUp(new DefaultMockServiceCivil());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSetBlocageRemboursementAutomatiquePMInconnueDansUnireg() throws Exception {

		final long noPM = 999999;

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) noPM);

		// on s'assure que l'entreprise n'existe pas dans la base
		assertNull(tiersDAO.get(noPM));

		// on s'assure que le code de blocage de remboursement est à true (valeur par défaut)
		// msi 23.09.2011: depuis la migration des coquilles vides, le service retourne null pour une PM inconnue
		{
			final Corporation corp = (Corporation) service.getParty(params);
			assertNull(corp);
		}

		// on change le code de remboursement
		final SetAutomaticReimbursementBlockingRequest paramsBloc = new SetAutomaticReimbursementBlockingRequest();
		paramsBloc.setBlocked(false);
		paramsBloc.setLogin(login);
		paramsBloc.setPartyNumber((int) noPM);
		try {
			service.setAutomaticReimbursementBlocking(paramsBloc);
			fail();
		}
		catch (WebServiceException e) {
			// changer le code de remboursement d'un PM inexistante n'est plus permis depuis que les coquilles vides des PMs ont été créées
			assertEquals("Le tiers n°999999 n'existe pas.", e.getMessage());
		}
	}

	@Test
	public void testSetBlocageRemboursementAutomatiquePMConnueDansUnireg() throws Exception {

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.NESTLE);
			}
		});

		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(MockEntrepriseFactory.NESTLE.getNumeroEntreprise());
			entreprise.setBlocageRemboursementAutomatique(true);
			return entreprise.getNumero();
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) idpm);

		// on s'assure que le code de blocage de remboursement est à true (valeur d'initialisation plus haut)
		{
			final Corporation pm = (Corporation) service.getParty(params);
			assertNotNull(pm);
			assertTrue(pm.isAutomaticReimbursementBlocked());
		}

		// on change le code de remboursement
		final SetAutomaticReimbursementBlockingRequest paramsBloc = new SetAutomaticReimbursementBlockingRequest();
		paramsBloc.setBlocked(false);
		paramsBloc.setLogin(login);
		paramsBloc.setPartyNumber((int) idpm);
		service.setAutomaticReimbursementBlocking(paramsBloc);

		// on s'assure que le code de blocage de remboursement est à maintenant à false
		{
			final Corporation pm = (Corporation) service.getParty(params);
			assertNotNull(pm);
			assertFalse(pm.isAutomaticReimbursementBlocked());
		}
	}

	/**
	 * [UNIREG-2302]
	 */
	@Test
	public void testGetAdresseEnvoiPersonneMorale() throws Exception {

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.BCV);
			}
		});

		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BCV.getNumeroEntreprise());
			return pm.getNumero();
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) idpm);
		params.getParts().add(PartyPart.ADDRESSES);

		// on s'assure que la formule d'appel d'une PM est bien renseignée
		{
			final Corporation bcv = (Corporation) service.getParty(params);
			assertNotNull(bcv);

			final List<Address> mailAddresses = bcv.getMailAddresses();
			assertNotNull(mailAddresses);

			final Address last = mailAddresses.get(mailAddresses.size() - 1);
			assertNotNull(last);

			final OrganisationMailAddressInfo organisation = last.getOrganisation();
			assertNotNull(organisation);
			assertEquals("Madame, Monsieur", organisation.getFormalGreeting());

			final FormattedAddress formatted = last.getFormattedAddress();
			assertEquals("Banque Cantonale Vaudoise", formatted.getLine1());
			assertEquals("Place Saint-François 14", formatted.getLine2());
			assertEquals("1000 Lausanne", formatted.getLine3());
			assertNull(formatted.getLine4());
			assertNull(formatted.getLine5());
			assertNull(formatted.getLine6());
		}
	}

	/**
	 * [UNIREG-2641] Vérifie que les fors fiscaux des PMs vaudoides possèdent bien le type d'autorité 'commune vaudoise'.
	 */
	@Test
	public void testGetForFiscauxPMVaudoise() throws Exception {

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.BCV);
			}
		});

		final long idPM = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BCV.getNumeroEntreprise());
			addRegimeFiscalVD(pm, date(1990, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(1990, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(1883, 6, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			return pm.getNumero();
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) idPM);
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		// on s'assure que le type d'autorité fiscale sur le for fiscal est bien hors-canton
		{
			final Corporation pm = (Corporation) service.getParty(params);
			assertNotNull(pm);

			final List<TaxResidence> tax = pm.getMainTaxResidences();
			assertNotNull(tax);

			final TaxResidence last = tax.get(tax.size() - 1);
			assertNotNull(last);
			assertEquals(MockCommune.Lausanne.getNoOFS(), last.getTaxationAuthorityFSOId());
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, last.getTaxationAuthorityType());
		}
	}

	/**
	 * [UNIREG-2641] Vérifie que les fors fiscaux des PMs hors-canton possèdent bien le type d'autorité 'commune hors-canton'.
	 */
	@Test
	public void testGetForFiscauxPMHorsCanton() throws Exception {

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.BANQUE_COOP);
			}
		});

		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BANQUE_COOP.getNumeroEntreprise());
			addRegimeFiscalVD(pm, date(1965, 5, 4), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(1965, 5, 4), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(1960, 1, 1), null, MockCommune.Bale);
			return pm.getNumero();
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) idpm);
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		// on s'assure que le type d'autorité fiscale sur le for fiscal est bien hors-canton
		{
			final Corporation pm = (Corporation) service.getParty(params);
			assertNotNull(pm);

			final List<TaxResidence> tax = pm.getMainTaxResidences();
			assertNotNull(tax);

			final TaxResidence last = tax.get(tax.size() - 1);
			assertNotNull(last);
			assertEquals(MockCommune.Bale.getNoOFS(), last.getTaxationAuthorityFSOId());
			assertEquals(TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY, last.getTaxationAuthorityType());
		}
	}

	/**
	 * [UNIREG-2641] Vérifie que les fors fiscaux des PMs hors-Suisse possèdent bien le type d'autorité 'pays hors-Suisse'.
	 */
	@Test
	public void testGetForFiscauxPMHorsSuisse() throws Exception {

		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, date(1965, 5, 4), null, "Oversees Ltd.");
			addFormeJuridique(pm, date(1965, 5, 4), null, FormeJuridiqueEntreprise.FILIALE_HS_NIRC);
			addRegimeFiscalVD(pm, date(1965, 5, 4), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(1965, 5, 4), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(1965, 5, 4), null, MockPays.Liechtenstein);
			return pm.getNumero();
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) idpm);
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		// on s'assure que le type d'autorité fiscale sur le for fiscal est bien hors-Suisse
		{
			final Corporation pm = (Corporation) service.getParty(params);
			assertNotNull(pm);

			final List<TaxResidence> tax = pm.getMainTaxResidences();
			assertNotNull(tax);

			final TaxResidence last = tax.get(tax.size() - 1);
			assertNotNull(last);
			assertEquals(MockPays.Liechtenstein.getNoOFS(), last.getTaxationAuthorityFSOId());
			assertEquals(TaxationAuthorityType.FOREIGN_COUNTRY, last.getTaxationAuthorityType());
		}
	}

	@Test
	public void testGetBatchPartyRequestAvecMelangePersonnesPhysiquesEtMorales() throws Exception {

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.BCV);
			}
		});

		final class Ids {
			long idPP;
			long idPM;
		}

		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BCV.getNumeroEntreprise());
			final PersonnePhysique pp = addNonHabitant("Cédric", "Digory", date(1980, 5, 30), Sexe.MASCULIN);
			final Ids ids1 = new Ids();
			ids1.idPP = pp.getNumero();
			ids1.idPM = pm.getNumero();
			return ids1;
		});

		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(login);
		params.getPartyNumbers().add((int) ids.idPM);
		params.getPartyNumbers().add((int) ids.idPP);
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		// appel du service
		final BatchParty result = service.getBatchParty(params);
		assertNotNull(result);

		final List<BatchPartyEntry> entries = result.getEntries();
		assertNotNull(entries);
		assertEquals(2, entries.size());

		// vérification qu'on a bien renvoyé les données sur les deux tiers
		final Set<Long> tiersRendus = new HashSet<>();
		for (BatchPartyEntry entry : entries) {
			tiersRendus.add((long) entry.getNumber());
		}
		assertEquals(2, tiersRendus.size());
		assertTrue(tiersRendus.contains(ids.idPM));
		assertTrue(tiersRendus.contains(ids.idPP));
	}
}

