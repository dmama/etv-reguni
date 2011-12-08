package ch.vd.uniregctb.webservices.party3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.BatchPartyEntry;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.OrganisationMailAddressInfo;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Sexe;

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
public class TiersWebServicePMTest extends WebserviceTest {

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
		servicePM.setUp(new DefaultMockServicePM());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSetBlocageRemboursementAutomatiquePMInconnueDansUnireg() throws Exception {

		final long noBCV = MockPersonneMorale.BCV.getNumeroEntreprise();

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) noBCV);

		// on s'assure que l'entreprise n'existe pas dans la base
		assertNull(tiersDAO.get(noBCV));

		// on s'assure que le code de blocage de remboursement est à true (valeur par défaut)
		// msi 23.09.2011: depuis la migration des coquilles vides, le service retourne null pour une PM inconnue
		{
			final Corporation bcv = (Corporation) service.getParty(params);
			assertNull(bcv);
		}

		// on change le code de remboursement
		final SetAutomaticReimbursementBlockingRequest paramsBloc = new SetAutomaticReimbursementBlockingRequest();
		paramsBloc.setBlocked(false);
		paramsBloc.setLogin(login);
		paramsBloc.setPartyNumber((int) noBCV);
		try {
			service.setAutomaticReimbursementBlocking(paramsBloc);
			fail();
		}
		catch (WebServiceException e) {
			// changer le code de remboursement d'un PM inexistante n'est plus permis depuis que les coquilles vides des PMs ont été créées
			assertEquals("Le tiers n°20222 n'existe pas.", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSetBlocageRemboursementAutomatiquePMConnueDansUnireg() throws Exception {

		final long noNestle = MockPersonneMorale.NestleSuisse.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntreprise(noNestle);
				entreprise.setBlocageRemboursementAutomatique(true);
				return null;
			}
		});

		// on s'assure que l'entreprise existe dans la base
		final Entreprise ent = (Entreprise) tiersDAO.get(noNestle);
		assertNotNull(ent);

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) noNestle);

		// on s'assure que le code de blocage de remboursement est à true (valeur d'initialisation plus haut)
		{
			final Corporation nestle = (Corporation) service.getParty(params);
			assertNotNull(nestle);
			assertTrue(nestle.isAutomaticReimbursementBlocked());
		}

		// on change le code de remboursement
		final SetAutomaticReimbursementBlockingRequest paramsBloc = new SetAutomaticReimbursementBlockingRequest();
		paramsBloc.setBlocked(false);
		paramsBloc.setLogin(login);
		paramsBloc.setPartyNumber((int) noNestle);
		service.setAutomaticReimbursementBlocking(paramsBloc);

		// on s'assure que le code de blocage de remboursement est à maintenant à false
		{
			final Corporation nestle = (Corporation) service.getParty(params);
			assertNotNull(nestle);
			assertFalse(nestle.isAutomaticReimbursementBlocked());
		}
	}

	/**
	 * [UNIREG-2302]
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAdresseEnvoiPersonneMorale() throws Exception {

		final long noBCV = MockPersonneMorale.BCV.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEntreprise(noBCV);
				return null;
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) noBCV);
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
			assertEquals("pa Comptabilité financière", formatted.getLine2());
			assertEquals("Saint-François, place 14", formatted.getLine3());
			assertEquals("1003 Lausanne Secteur de dist.", formatted.getLine4());
			assertNull(formatted.getLine5());
			assertNull(formatted.getLine6());
		}
	}

	/**
	 * [UNIREG-1974] Vérifie que l'adresse de la fiduciaire Jal Holding utilise bien les trois lignes de la raison sociale et non pas la raison sociale abbrégée.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAdresseEnvoiPersonneMorale2() throws Exception {

		final long noJal = MockPersonneMorale.JalHolding.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEntreprise(noJal);
				return null;
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) noJal);
		params.getParts().add(PartyPart.ADDRESSES);

		// on s'assure que la formule d'appel d'une PM est bien renseignée
		{
			final Corporation jal = (Corporation) service.getParty(params);
			assertNotNull(jal);

			final List<Address> mailAddresses = jal.getMailAddresses();
			assertNotNull(mailAddresses);

			final Address last = mailAddresses.get(mailAddresses.size() - 1);
			assertNotNull(last);

			final OrganisationMailAddressInfo organisation = last.getOrganisation();
			assertNotNull(organisation);
			assertEquals("Madame, Monsieur", organisation.getFormalGreeting());

			final FormattedAddress formatted = last.getFormattedAddress();
			assertEquals("Jal holding S.A.", formatted.getLine1());
			assertEquals("en liquidation", formatted.getLine2());
			assertEquals("pa Fidu. Commerce & Industrie", formatted.getLine3());
			assertEquals("Avenue de la Gare 10", formatted.getLine4());
			assertEquals("1003 Lausanne", formatted.getLine5());
			assertNull(formatted.getLine6());
		}
	}

	/**
	 * [UNIREG-1974] Vérifie que l'adresse de la PM Evian-Russie tient bien sur 6 lignes et que le complément d'adresse est ignoré
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAdresseEnvoiPersonneMoraleOptionnaliteComplement() throws Exception {

		final long noEvian = MockPersonneMorale.EvianRussie.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEntreprise(noEvian);
				return null;
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int)noEvian);
		params.getParts().add(PartyPart.ADDRESSES);

		// on s'assure que la formule d'appel d'une PM est bien renseignée
		{
			final Corporation evian = (Corporation) service.getParty(params);
			assertNotNull(evian);

			final List<Address> mailAddresses = evian.getMailAddresses();
			assertNotNull(mailAddresses);

			final Address last = mailAddresses.get(mailAddresses.size() - 1);
			assertNotNull(last);

			final OrganisationMailAddressInfo organisation = last.getOrganisation();
			assertNotNull(organisation);
			assertEquals("Madame, Monsieur", organisation.getFormalGreeting());

			final FormattedAddress formatted = last.getFormattedAddress();
			assertEquals("Distributor (Evian Water)", formatted.getLine1());
			assertEquals("LLC PepsiCo Holdings", formatted.getLine2());
			assertEquals("Free Economic Zone Sherrizone", formatted.getLine3());

			// [UNIREG-1974] le complément est ignoré pour que l'adresse tienne sur 6 lignes
			// assertEquals("p.a. Aleksey Fyodorovich Karamazov", formatted.getLineXXX());

			assertEquals("Solnechnogorsk Dist.", formatted.getLine4());
			assertEquals("141580 Moscow region", formatted.getLine5());
			assertEquals("Russie", formatted.getLine6());
		}
	}

	/**
	 * [UNIREG-2641] Vérifie que les fors fiscaux des PMs vaudoides possèdent bien le type d'autorité 'commune vaudoise'.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForFiscauxPMVaudoise() throws Exception {

		final long noPM = MockPersonneMorale.BCV.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEntreprise(noPM);
				return null;
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) noPM);
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
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForFiscauxPMHorsCanton() throws Exception {

		final long noPM = MockPersonneMorale.BanqueCoopBale.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addEntreprise(noPM);
				return null;
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) noPM);
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
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForFiscauxPMHorsSuisse() throws Exception {

		final long noPM = MockPersonneMorale.KhatAnstalt.getNumeroEntreprise();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEntreprise(noPM);
				return null;
			}
		});

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) noPM);
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		// on s'assure que le type d'autorité fiscale sur le for fiscal est bien hors-canton
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
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchPartyRequestAvecMelangePersonnesPhysiquesEtMorales() throws Exception {

		final long noPM = MockPersonneMorale.BCV.getNumeroEntreprise();

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addEntreprise(noPM);
				final PersonnePhysique pp = addNonHabitant("Cédric", "Digory", date(1980, 5, 30), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		final GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(login);
		params.getPartyNumbers().add((int) noPM);
		params.getPartyNumbers().add((int) ppId);
		params.getParts().add(PartyPart.TAX_RESIDENCES);

		// appel du service
		final BatchParty result = service.getBatchParty(params);
		assertNotNull(result);

		final List<BatchPartyEntry> entries = result.getEntries();
		assertNotNull(entries);
		assertEquals(2, entries.size());

		// vérification qu'on a bien renvoyé les données sur les deux tiers
		final Set<Long> tiersRendus = new HashSet<Long>();
		for (BatchPartyEntry entry : entries) {
			tiersRendus.add((long) entry.getNumber());
		}
		assertEquals(2, tiersRendus.size());
		assertTrue(tiersRendus.contains(noPM));
		assertTrue(tiersRendus.contains(ppId));
	}
}

