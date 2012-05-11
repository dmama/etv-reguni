package ch.vd.uniregctb.webservices.tiers2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHistoEntry;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.SetTiersBlocRembAuto;

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

	private TiersWebService service;
	private UserLogin login;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(TiersWebService.class, "tiersService2Bean");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		login = new UserLogin("iamtestuser", 22);
		serviceCivil.setUp(new DefaultMockServiceCivil());
		servicePM.setUp(new DefaultMockServicePM());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSetBlocageRemboursementAutomatiquePMInconnueDansUnireg() throws Exception {

		final long noBCV = MockPersonneMorale.BCV.getNumeroEntreprise();

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noBCV;

		// on s'assure que l'entreprise n'existe pas dans la base
		assertNull(tiersDAO.get(noBCV));

		// on s'assure que le code de blocage de remboursement est à true (valeur par défaut)
		// msi 23.09.2011: depuis la migration des coquilles vides, le service retourne null pour une PM inconnue
		{
			final PersonneMorale bcv = (PersonneMorale) service.getTiers(params);
			assertNull(bcv);
		}

		// on change le code de remboursement
		final SetTiersBlocRembAuto paramsBloc = new SetTiersBlocRembAuto();
		paramsBloc.blocage = false;
		paramsBloc.login = login;
		paramsBloc.tiersNumber = noBCV;
		try {
			service.setTiersBlocRembAuto(paramsBloc);
			fail();
		}
		catch (BusinessException e) {
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

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noNestle;

		// on s'assure que le code de blocage de remboursement est à true (valeur d'initialisation plus haut)
		{
			final PersonneMorale nestle = (PersonneMorale) service.getTiers(params);
			assertNotNull(nestle);
			assertTrue(nestle.blocageRemboursementAutomatique);
		}

		// on change le code de remboursement
		final SetTiersBlocRembAuto paramsBloc = new SetTiersBlocRembAuto();
		paramsBloc.blocage = false;
		paramsBloc.login = login;
		paramsBloc.tiersNumber = noNestle;
		service.setTiersBlocRembAuto(paramsBloc);

		// on s'assure que le code de blocage de remboursement est à maintenant à false
		{
			final PersonneMorale nestle = (PersonneMorale) service.getTiers(params);
			assertNotNull(nestle);
			assertFalse(nestle.blocageRemboursementAutomatique);
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

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noBCV;
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.ADRESSES_ENVOI);

		// on s'assure que la formule d'appel d'une PM est bien renseignée
		{
			final PersonneMorale bcv = (PersonneMorale) service.getTiers(params);
			assertNotNull(bcv);
			assertNotNull(bcv.adresseEnvoi);

			// l'adresse d'envoi n'a pas de salutations
			assertNull(bcv.adresseEnvoi.salutations);
			assertEquals("Banque Cantonale Vaudoise", bcv.adresseEnvoi.ligne1);
			assertEquals("pa Comptabilité financière", bcv.adresseEnvoi.ligne2);
			assertEquals("Saint-François, place 14", bcv.adresseEnvoi.ligne3);
			assertEquals("1003 Lausanne Secteur de dist.", bcv.adresseEnvoi.ligne4);
			assertNull(bcv.adresseEnvoi.ligne5);
			assertNull(bcv.adresseEnvoi.ligne6);

			// par contre, la formule d'appel est renseignée
			assertEquals("Madame, Monsieur", bcv.adresseEnvoi.formuleAppel);
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

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noJal;
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.ADRESSES_ENVOI);

		// on s'assure que la formule d'appel d'une PM est bien renseignée
		{
			final PersonneMorale jal = (PersonneMorale) service.getTiers(params);
			assertNotNull(jal);
			assertNotNull(jal.adresseEnvoi);

			// l'adresse d'envoi n'a pas de salutations
			assertNull(jal.adresseEnvoi.salutations);
			assertEquals("Jal holding S.A.", jal.adresseEnvoi.ligne1);
			assertEquals("en liquidation", jal.adresseEnvoi.ligne2);
			assertEquals("pa Fidu. Commerce & Industrie", jal.adresseEnvoi.ligne3);
			assertEquals("Avenue de la Gare 10", jal.adresseEnvoi.ligne4);
			assertEquals("1003 Lausanne", jal.adresseEnvoi.ligne5);
			assertNull(jal.adresseEnvoi.ligne6);

			// par contre, la formule d'appel est renseignée
			assertEquals("Madame, Monsieur", jal.adresseEnvoi.formuleAppel);
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

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noEvian;
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.ADRESSES_ENVOI);

		// on s'assure que la formule d'appel d'une PM est bien renseignée
		{
			final PersonneMorale evian = (PersonneMorale) service.getTiers(params);
			assertNotNull(evian);
			assertNotNull(evian.adresseEnvoi);

			// l'adresse d'envoi n'a pas de salutations
			assertNull(evian.adresseEnvoi.salutations);
			assertEquals("Distributor (Evian Water)", evian.adresseEnvoi.ligne1);
			assertEquals("LLC PepsiCo Holdings", evian.adresseEnvoi.ligne2);
			assertEquals("Free Economic Zone Sherrizone", evian.adresseEnvoi.ligne3);

			// [UNIREG-1974] le complément est ignoré pour que l'adresse tienne sur 6 lignes
			// assertEquals("p.a. Aleksey Fyodorovich Karamazov", evian.adresseEnvoi.ligneXXX);

			assertEquals("Solnechnogorsk Dist.", evian.adresseEnvoi.ligne4);
			assertEquals("141580 Moscow region", evian.adresseEnvoi.ligne5);
			assertEquals("Russie", evian.adresseEnvoi.ligne6);

			// par contre, la formule d'appel est renseignée
			assertEquals("Madame, Monsieur", evian.adresseEnvoi.formuleAppel);
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

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noPM;
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.FORS_FISCAUX);

		// on s'assure que le type d'autorité fiscale sur le for fiscal est bien hors-canton
		{
			final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
			assertNotNull(pm);

			final ForFiscal ffp = pm.forFiscalPrincipal;
			assertNotNull(ffp);
			assertEquals(MockCommune.Lausanne.getNoOFS(), ffp.noOfsAutoriteFiscale);
			assertEquals(ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.typeAutoriteFiscale);
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

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noPM;
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.FORS_FISCAUX);

		// on s'assure que le type d'autorité fiscale sur le for fiscal est bien hors-canton
		{
			final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
			assertNotNull(pm);

			final ForFiscal ffp = pm.forFiscalPrincipal;
			assertNotNull(ffp);
			assertEquals(MockCommune.Bale.getNoOFS(), ffp.noOfsAutoriteFiscale);
			assertEquals(ForFiscal.TypeAutoriteFiscale.COMMUNE_HC, ffp.typeAutoriteFiscale);
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

		final GetTiers params = new GetTiers();
		params.date = null;
		params.login = login;
		params.tiersNumber = noPM;
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.FORS_FISCAUX);

		// on s'assure que le type d'autorité fiscale sur le for fiscal est bien hors-canton
		{
			final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
			assertNotNull(pm);

			final ForFiscal ffp = pm.forFiscalPrincipal;
			assertNotNull(ffp);
			assertEquals(MockPays.Liechtenstein.getNoOFS(), ffp.noOfsAutoriteFiscale);
			assertEquals(ForFiscal.TypeAutoriteFiscale.PAYS_HS, ffp.typeAutoriteFiscale);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchTiersAvecMelangePersonnesPhysiquesEtMorales() throws Exception {

		final long noPM = MockPersonneMorale.BCV.getNumeroEntreprise();

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addEntreprise(noPM);
				final PersonnePhysique pp = addNonHabitant("Cédric", "Digory", date(1980, 5, 30), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		final GetBatchTiers params = new GetBatchTiers();
		params.date = null;
		params.login = login;
		params.tiersNumbers = new HashSet<Long>();
		params.tiersNumbers.add(noPM);
		params.tiersNumbers.add(ppId);
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.FORS_FISCAUX);

		// appel du service
		final BatchTiers result = service.getBatchTiers(params);
		assertNotNull(result);
		assertFalse(result.isEmpty());

		final List<BatchTiersEntry> entries = result.entries;
		assertNotNull(entries);
		assertEquals(2, entries.size());

		// vérification qu'on a bien renvoyé les données sur les deux tiers
		final Set<Long> tiersRendus = new HashSet<Long>();
		for (BatchTiersEntry entry : entries) {
			tiersRendus.add(entry.number);
		}
		assertEquals(2, tiersRendus.size());
		assertTrue(tiersRendus.contains(noPM));
		assertTrue(tiersRendus.contains(ppId));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchTiersHistoAvecMelangePersonnesPhysiquesEtMorales() throws Exception {

		final long noPM = MockPersonneMorale.BCV.getNumeroEntreprise();

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addEntreprise(noPM);
				final PersonnePhysique pp = addNonHabitant("Cédric", "Digory", date(1980, 5, 30), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		final GetBatchTiersHisto params = new GetBatchTiersHisto();
		params.login = login;
		params.tiersNumbers = new HashSet<Long>();
		params.tiersNumbers.add(noPM);
		params.tiersNumbers.add(ppId);
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.FORS_FISCAUX);

		// appel du service
		final BatchTiersHisto result = service.getBatchTiersHisto(params);
		assertNotNull(result);
		assertFalse(result.isEmpty());

		final List<BatchTiersHistoEntry> entries = result.entries;
		assertNotNull(entries);
		assertEquals(2, entries.size());

		// vérification qu'on a bien renvoyé les données sur les deux tiers
		final Set<Long> tiersRendus = new HashSet<Long>();
		for (BatchTiersHistoEntry entry : entries) {
			tiersRendus.add(entry.number);
		}
		assertEquals(2, tiersRendus.size());
		assertTrue(tiersRendus.contains(noPM));
		assertTrue(tiersRendus.contains(ppId));
	}
}

