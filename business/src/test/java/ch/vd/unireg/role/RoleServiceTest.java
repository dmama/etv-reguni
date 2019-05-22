package ch.vd.unireg.role;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.LocalisationFiscale;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RoleServiceTest extends BusinessTest {

	private RoleServiceImpl roleService;
	private AssujettissementService assujettissementService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		roleService = new RoleServiceImpl();
		roleService.setAdresseService(getBean(AdresseService.class, "adresseService"));
		roleService.setAssujettissementService(assujettissementService);
		roleService.setInfraService(serviceInfra);
		roleService.setRoleHelper(new RoleHelper(transactionManager, hibernateTemplate, tiersService));
		roleService.setTiersService(tiersService);
		roleService.setTransactionManager(transactionManager);
	}

	/**
	 * Méthode utilitaire pour transformer une extraction sous forme de liste en une map indexée par numéro de contribuable
	 * @param extraction liste des extractions
	 * @param <T> type de donnée extraite
	 * @return map indexée par numéro de contribuable
	 */
	private static <T extends RoleData> Map<Long, T> extractionToMap(List<T> extraction) {
		return extraction.stream()
				.collect(Collectors.toMap(data -> data.noContribuable, Function.identity()));
	}

	@Test
	public void testRun() throws Exception {

		serviceCivil.setUp(new DefaultMockIndividuConnector());

		class Ids {
			public Long paul;
			public Long incognito;
			public Long raoul;
			public Long didier;
			public Long laurent;
			public Long arnold;
			public Long victor;
			public Long albertine;
			public Long geo;
			public Long donald;
			public Long johnny;
			public Long tyler;
			public Long pascal;
			public Long marc;
			public Long louis;
			public Long albert;
			public Long georges;
			public Long marie;
			public Long jean;
			public Long tom;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			ids.paul = newCtbVaudoisOrdinaire(MockCommune.Lausanne).getNumero();
			ids.incognito = newCtbVaudoisOrdinaireDepuis2007(MockCommune.Lausanne).getNumero();
			ids.raoul = newCtbVaudoisOrdinairePartiHorsCantonEn2007(MockCommune.Lausanne).getNumero();
			ids.didier = newCtbVaudoisOrdinairePartiHorsCantonEn2006(MockCommune.Lausanne).getNumero();
			ids.laurent = newCtbVaudoisOrdinairePartiHorsCantonEn2008(MockCommune.Lausanne).getNumero();
			ids.arnold = newCtbVaudoisSourcierMixte(MockCommune.Lausanne).getNumero();
			ids.victor = newCtbVaudoisSourcier(MockCommune.Lausanne).getNumero();
			ids.albertine = newCtbVaudoisSourcierGris(MockCommune.Lausanne).getNumero();
			ids.geo = newCtbHorsCantonEtImmeuble(MockCommune.Lausanne).getNumero();
			ids.donald = newCtbHorsCantonEtDeuxImmeubles(MockCommune.Lausanne, MockCommune.Lausanne).getNumero();
			ids.johnny = newCtbHorsCantonEtImmeubleVenduEn2007(MockCommune.Lausanne).getNumero();
			ids.tyler = newCtbHorsCantonEtActiviteIndStoppeeEn2007(MockCommune.Lausanne).getNumero();
			ids.pascal = newCtbOrdinaireVaudoisEtImmeuble(MockCommune.Lausanne, MockCommune.Cossonay).getNumero();
			ids.marc = newCtbDiplomateSuisse(MockCommune.Lausanne).getNumero();
			ids.louis = newCtbVaudoisOrdinairePartiHorsCantonEtImmeuble(MockCommune.Lausanne, MockCommune.Cossonay).getNumero();
			ids.albert = newCtbVaudoisOrdinairePartiHorsSuisseEtImmeuble(MockCommune.Lausanne, MockCommune.Cossonay).getNumero();
			ids.georges = newCtbHorsCantonDeuxImmeublesNonChevauchant(MockCommune.Lausanne, MockCommune.Lausanne).getNumero();
			ids.marie = newCtbVaudoisPartiHorsCantonTrenteEtUnDecembre(MockCommune.Lausanne).getNumero();
			ids.jean = newCtbVaudoisPartiHorsSuisseTrenteEtUnDecembre(MockCommune.Lausanne).getNumero();
			ids.tom = newCtbHorsSuisseImmeubleVenduTrenteEtUnDecembre(MockCommune.Lausanne).getNumero();
			return null;
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2007, 1, null, null);
		assertNotNull(results);
		assertEquals(19, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(5, results.ignores.size()); // le diplomate, le sourcier gris et les trois HC sans rattachement économique fin 2007
		assertEquals(2, results.extraction.size());

		// les ignorés
		{
			assertIgnore(ids.raoul, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, results.ignores.get(0));
			assertIgnore(ids.albertine, RoleResults.RaisonIgnore.SOURCIER_GRIS, results.ignores.get(1));
			assertIgnore(ids.johnny, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, results.ignores.get(2));
			assertIgnore(ids.tyler, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, results.ignores.get(3));
			assertIgnore(ids.marc, RoleResults.RaisonIgnore.DIPLOMATE_SUISSE, results.ignores.get(4));
		}

		{
			final int ofsCommune = MockCommune.Lausanne.getNoOFS();
			final List<RolePPData> infoCommune = results.extraction.get(ofsCommune);
			assertNotNull(infoCommune);
			final Map<Long, RolePPData> infoParContribuable = extractionToMap(infoCommune);

			final RolePPData infoPaul = infoParContribuable.get(ids.paul);
			final RolePPData infoIncognito = infoParContribuable.get(ids.incognito);
			final RolePPData infoRaoul = infoParContribuable.get(ids.raoul);
			final RolePPData infoDidier = infoParContribuable.get(ids.didier);
			final RolePPData infoLaurent = infoParContribuable.get(ids.laurent);
			final RolePPData infoArnold = infoParContribuable.get(ids.arnold);
			final RolePPData infoVictor = infoParContribuable.get(ids.victor);
			final RolePPData infoAlbertine = infoParContribuable.get(ids.albertine);
			final RolePPData infoGeo = infoParContribuable.get(ids.geo);
			final RolePPData infoDonald = infoParContribuable.get(ids.donald);
			final RolePPData infoJohnny = infoParContribuable.get(ids.johnny);
			final RolePPData infoTyler = infoParContribuable.get(ids.tyler);
			final RolePPData infoPascal = infoParContribuable.get(ids.pascal);
			final RolePPData infoMarc = infoParContribuable.get(ids.marc);
			final RolePPData infoLouis = infoParContribuable.get(ids.louis);
			final RolePPData infoAlbert = infoParContribuable.get(ids.albert);
			final RolePPData infoGeorges = infoParContribuable.get(ids.georges);
			final RolePPData infoMarie = infoParContribuable.get(ids.marie);
			final RolePPData infoJean = infoParContribuable.get(ids.jean);
			final RolePPData infoTom = infoParContribuable.get(ids.tom);

			assertInfo(ids.paul, RoleData.TypeContribuable.ORDINAIRE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Duchêne", "Paul")), Collections.emptyList(), infoPaul);
			assertInfo(ids.incognito, RoleData.TypeContribuable.ORDINAIRE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Nito", "Incog")), Collections.emptyList(), infoIncognito);
			assertNull(infoRaoul);
			assertNull(infoDidier);
			assertInfo(ids.laurent, RoleData.TypeContribuable.ORDINAIRE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Du", "Laurent")), Collections.emptyList(), infoLaurent);
			assertInfo(ids.arnold, RoleData.TypeContribuable.MIXTE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Duplat", "Arnold")), Collections.emptyList(), infoArnold);
			assertInfo(ids.victor, RoleData.TypeContribuable.SOURCE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Duplat", "Victor")), Collections.emptyList(), infoVictor);
			assertNull(infoAlbertine);
			assertInfo(ids.geo, RoleData.TypeContribuable.HORS_CANTON, ofsCommune, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Neuchatel.getNoOFS(), Collections.singletonList(new NomPrenom("Trouverien", "Geo")), Collections.emptyList(), infoGeo);
			assertInfo(ids.donald, RoleData.TypeContribuable.HORS_CANTON, ofsCommune, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Neuchatel.getNoOFS(), Collections.singletonList(new NomPrenom("Trouverien", "Donald")), Collections.emptyList(), infoDonald);
			assertNull(infoJohnny);
			assertNull(infoTyler);
			assertInfo(ids.pascal, RoleData.TypeContribuable.ORDINAIRE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Duchêne", "Paul")), Collections.emptyList(), infoPascal);
			assertNull(infoMarc);
			assertNull(infoLouis);
			assertNull(infoAlbert);
			assertInfo(ids.georges, RoleData.TypeContribuable.HORS_CANTON, ofsCommune, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Neuchatel.getNoOFS(), Collections.singletonList(new NomPrenom("Trouverien", "Georges")), Collections.emptyList(), infoGeorges);
			assertInfo(ids.marie, RoleData.TypeContribuable.ORDINAIRE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Coller", "Marie")), Collections.emptyList(), infoMarie);
			assertInfo(ids.jean, RoleData.TypeContribuable.ORDINAIRE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Coller", "Jean")), Collections.emptyList(), infoJean);
			assertInfo(ids.tom, RoleData.TypeContribuable.HORS_SUISSE, ofsCommune, TypeAutoriteFiscale.PAYS_HS, MockPays.Albanie.getNoOFS(), Collections.singletonList(new NomPrenom("Cruise", "Tom")), Collections.emptyList(), infoTom);

			assertEquals(12, infoCommune.size());
			assertEquals(12, infoParContribuable.size());
		}

		{
			final int ofsCommune = MockCommune.Cossonay.getNoOFS();
			final List<RolePPData> infoCommune = results.extraction.get(ofsCommune);
			assertNotNull(infoCommune);
			final Map<Long, RolePPData> infoParContribuable = extractionToMap(infoCommune);

			final RolePPData infoPascal = infoParContribuable.get(ids.pascal);
			final RolePPData infoMarc = infoParContribuable.get(ids.marc);
			final RolePPData infoLouis = infoParContribuable.get(ids.louis);
			final RolePPData infoAlbert = infoParContribuable.get(ids.albert);

			assertNull(infoPascal);
			assertNull(infoMarc);
			assertInfo(ids.louis, RoleData.TypeContribuable.HORS_CANTON, ofsCommune, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Bern.getNoOFS(), Collections.singletonList(new NomPrenom("Coller", "Louis")), Collections.emptyList(), infoLouis);
			assertInfo(ids.albert, RoleData.TypeContribuable.HORS_SUISSE, ofsCommune, TypeAutoriteFiscale.PAYS_HS, MockPays.PaysInconnu.getNoOFS(), Collections.singletonList(new NomPrenom("Coller", "Albert")), Collections.emptyList(), infoAlbert);

			assertEquals(2, infoCommune.size());
			assertEquals(2, infoParContribuable.size());
		}
	}

	@Test
	public void testRunHorsSuisseRevenuDansMemeCommuneLaMemeAnnee() throws Exception {

		class Ids {
			public Long benjamin;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			ids.benjamin = newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune.Lausanne).getNumero();
			return null;
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2007, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		{
			final List<RolePPData> infoLausanne = results.extraction.get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);
			assertEquals(1, infoLausanne.size());

			final RolePPData data = infoLausanne.get(0);
			assertNotNull(data);
			assertInfo(ids.benjamin, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("TientPasEnPlace", "Benjamin")), Collections.emptyList(), data);
		}
	}

	@Test
	public void testDepartHS() throws Exception {

		final int anneeRoles = 2015;
		final RegDate dateDepart = date(anneeRoles, 6, 2);

		final long idpp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Francisco", "Delapierre", date(1964, 8, 12), Sexe.MASCULIN);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, dateDepart, MotifFor.DEPART_HS, MockCommune.Aigle);
			addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Japon);
			return pp.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(anneeRoles, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		{
			final List<RolePPData> infoAigle = results.extraction.get(MockCommune.Aigle.getNoOFS());
			assertNotNull(infoAigle);
			assertEquals(1, infoAigle.size());

			final RolePPData data = infoAigle.get(0);
			assertNotNull(data);
			assertInfo(idpp, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MockPays.Japon.getNoOFS(), Collections.singletonList(new NomPrenom("Delapierre", "Francisco")), Collections.emptyList(), data);
		}
	}

	@Test
	public void testRunHorsSuisseRevenuDansAutreCommuneLaMemeAnnee() throws Exception {

		final long idCtb = doInNewTransaction(status -> {
			final Contribuable ctb = newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune.Lausanne, MockCommune.Bussigny);
			return ctb.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2007, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		{
			final List<RolePPData> infoBussigny = results.extraction.get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infoBussigny);
			assertEquals(1, infoBussigny.size());

			final RolePPData data = infoBussigny.get(0);
			assertNotNull(data);
			assertInfo(idCtb, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), Collections.singletonList(new NomPrenom("TientPasEnPlace", "Benjamin")), Collections.emptyList(), data);
		}
	}

	@Test
	public void testRunSourcierPartiHorsSuisseEtRevenuDansAutreCommuneLaMemeAnnee() throws Exception {

		final long noIndividu = 183747L;
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Lasource", "Marie", Sexe.FEMININ);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2007, 10, 1), null);
			}
		});

		final long idCtb = doInNewTransaction(status -> {
			final Contribuable ctb = newCtbVaudoisSourcierPartiHorsSuisseEtRevenuDansLaMemeAnnee(noIndividu, MockCommune.Lausanne, MockCommune.Bussigny);
			return ctb.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2007, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		{
			final List<RolePPData> infoBussigny = results.extraction.get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infoBussigny);
			assertEquals(1, infoBussigny.size());

			final RolePPData data = infoBussigny.get(0);
			assertNotNull(data);
			assertInfo(idCtb, RoleData.TypeContribuable.SOURCE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), Collections.singletonList(new NomPrenom("Lasource", "Marie")), Collections.emptyList(), data);
		}
	}

	@Test
	public void testRunSourcierPartiDansAutreCommuneLaMemeAnneePuisRetourPremiereCommuneDebutAnneeSuivante() throws Exception {

		final long noIndividu = 183747L;
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Lasource", "Marie", Sexe.FEMININ);
				ind.setNouveauNoAVS("7568409992270");
			}
		});

		final long idMarie = doInNewTransaction(status -> {
			final PersonnePhysique m = addHabitant(noIndividu);
			addForPrincipal(m, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			addForPrincipal(m, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne, ModeImposition.SOURCE);
			addForPrincipal(m, date(2007, 10, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, ModeImposition.SOURCE);
			addForPrincipal(m, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, ModeImposition.SOURCE);
			return m.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2007, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		{
			final List<RolePPData> infoBussigny = results.extraction.get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infoBussigny);
			assertEquals(1, infoBussigny.size());

			final RolePPData data = infoBussigny.get(0);
			assertNotNull(data);
			assertInfo(idMarie, RoleData.TypeContribuable.SOURCE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), Collections.singletonList(new NomPrenom("Lasource", "Marie")), Collections.singletonList("7568409992270"), data);
		}
	}

	@Test
	public void testRunSourcierPartiHorsSuisseEtRevenuDansAutreCommuneLaMemeAnneePuisRetourPremiereCommuneAnneeSuivante() throws Exception {

		final long noIndividu = 183747L;
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Lasource", "Marie", Sexe.FEMININ);
			}
		});

		final long idMarie = doInNewTransaction(status -> {
			final PersonnePhysique m = addHabitant(noIndividu);
			addForPrincipal(m, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			addForPrincipal(m, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne, ModeImposition.SOURCE);
			addForPrincipal(m, date(2007, 10, 1), MotifFor.ARRIVEE_HS, date(2008, 5, 15), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, ModeImposition.SOURCE);
			addForPrincipal(m, date(2008, 5, 16), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, ModeImposition.SOURCE);
			return m.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2007, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		{
			final List<RolePPData> infoBussigny = results.extraction.get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infoBussigny);
			assertEquals(1, infoBussigny.size());

			final RolePPData data = infoBussigny.get(0);
			assertNotNull(data);
			assertInfo(idMarie, RoleData.TypeContribuable.SOURCE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), Collections.singletonList(new NomPrenom("Lasource", "Marie")), Collections.emptyList(), data);
		}
	}

	@Test
	public void testSourcierPartiHCDansAnneeRoles() throws Exception {

		final long noIndividu = 463467849L;
		final int pfRoles = 2011;
		final RegDate dateArrivee = date(pfRoles - 2, 1, 1);
		final RegDate dateDepartHc = date(pfRoles, 3, 12);

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Drüstiene", "Helmut", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, dateDepartHc);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, dateDepartHc);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Neuchatel.RueDesBeauxArts, null, dateDepartHc.getOneDayAfter(), null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateDepartHc, MotifFor.DEPART_HC, MockCommune.Cossonay, ModeImposition.SOURCE);
			addForPrincipal(pp, dateDepartHc.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(pfRoles, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(1, results.ignores.size());
		assertEquals(0, results.extraction.size());

		// ignorés
		{
			assertIgnore(ppId, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, results.ignores.get(0));
		}
	}

	/**
	 * Test pour UNIREG-2777
	 */
	@Test
	public void testRunCommunesCtbHorsCantonAvecPlusieursForsSecondaires() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final Contribuable ctb = getCtbHorsCantonAvecDeuxForsImmeublesOuvertsPourJIRA2777();
			return ctb.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2008, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		{
			final List<RolePPData> infoCommune = results.extraction.get(MockCommune.RomainmotierEnvy.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.size());

			final RolePPData data = infoCommune.get(0);
			assertNotNull(data);
			assertInfo(ppId, RoleData.TypeContribuable.HORS_CANTON, MockCommune.RomainmotierEnvy.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Bern.getNoOFS(), Collections.singletonList(new NomPrenom("Brindacier", "Fifi")), Collections.emptyList(), data);
		}
	}

	@Test
	public void testRunOID() throws Exception {

		serviceCivil.setUp(new DefaultMockIndividuConnector());

		class Ids {
			public Long paul;
			public Long raoul;
			public Long didier;
			public Long arnold;
			public Long victor;
			public Long balthazar;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			ids.paul = newCtbOrdinaireVaudoisEtImmeuble(MockCommune.Lausanne, MockCommune.Aubonne).getNumero();
			ids.raoul = newCtbOrdinaireVaudoisEtImmeuble(MockCommune.Renens, MockCommune.Lausanne).getNumero();
			ids.didier = newCtbOrdinaireAvecDemenagement(MockCommune.Lausanne, MockCommune.Aubonne).getNumero();
			ids.arnold = newCtbOrdinaireAvecDemenagement(MockCommune.Renens, MockCommune.Lausanne).getNumero();
			ids.victor = newCtbOrdinaireAvecDemenagementEnGardantImmeuble(MockCommune.Lausanne, MockCommune.Croy, MockCommune.Renens).getNumero();
			ids.balthazar = newCtbOrdinaireAvecDemenagementAnterieur(MockCommune.Renens, MockCommune.Lausanne).getNumero();
			return null;
		});

		final RolePPOfficesResults results = roleService.produireRolePPOffices(2007, 1, MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), null);
		assertNotNull(results);
		assertEquals(6, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(2, results.ignores.size());        // victor et didier n'habitent plus sur le territoire de l'OID
		assertEquals(1, results.extraction.size());

		// les ignorés
		{
			final RoleResults.RoleIgnore ignore = results.ignores.get(0);
			assertNotNull(ignore);
			assertIgnore(ids.didier, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE_SUR_COMMUNE, ignore);
		}
		{
			final RoleResults.RoleIgnore ignore = results.ignores.get(1);
			assertNotNull(ignore);
			assertIgnore(ids.victor, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE_SUR_COMMUNE, ignore);
		}

		// les extraits

		assertEquals(Collections.singleton(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm()), results.extraction.keySet());
		final List<RolePPData> infoOid = results.extraction.get(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
		assertNotNull(infoOid);
		assertEquals(4, infoOid.size());

		final Map<Long, RolePPData> infoParContribuable = extractionToMap(infoOid);
		assertEquals(4, infoParContribuable.size());

		final RolePPData infoPaul = infoParContribuable.get(ids.paul);
		final RolePPData infoRaoul = infoParContribuable.get(ids.raoul);
		final RolePPData infoDidier = infoParContribuable.get(ids.didier);
		final RolePPData infoArnold = infoParContribuable.get(ids.arnold);
		final RolePPData infoVictor = infoParContribuable.get(ids.victor);
		final RolePPData infoBalthazar = infoParContribuable.get(ids.balthazar);

		assertInfo(ids.paul, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Duchêne", "Paul")), Collections.emptyList(), infoPaul);
		assertInfo(ids.raoul, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Renens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), Collections.singletonList(new NomPrenom("Duchêne", "Paul")), Collections.emptyList(), infoRaoul);
		assertNull(infoDidier);
		assertInfo(ids.arnold, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Tutu", "Turlu")), Collections.emptyList(), infoArnold);
		assertNull(infoVictor);
		assertInfo(ids.balthazar, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), Collections.singletonList(new NomPrenom("Tutu", "Turlu")), Collections.emptyList(), infoBalthazar);
	}

	private static void assertInfo(long id, RoleData.TypeContribuable type, int ofsCommune, TypeAutoriteFiscale tafDomicile, Integer noOfsDomicile, RoleData info) {
		assertNotNull(info);
		assertEquals(id, info.noContribuable);
		assertEquals(type, info.typeContribuable);
		assertEquals(ofsCommune, info.noOfsCommune);
		assertEquals(tafDomicile, Optional.ofNullable(info.domicileFiscal).map(LocalisationFiscale::getTypeAutoriteFiscale).orElse(null));
		assertEquals(noOfsDomicile, Optional.ofNullable(info.domicileFiscal).map(LocalisationFiscale::getNumeroOfsAutoriteFiscale).orElse(null));
	}

	private static void assertInfo(long id, RoleData.TypeContribuable type, int ofsCommune, TypeAutoriteFiscale tafDomicile, Integer noOfsDomicile, List<NomPrenom> nomsPrenoms, List<String> nosAvs, RolePPData info) {
		assertInfo(id, type, ofsCommune, tafDomicile, noOfsDomicile, info);
		assertEquals(nomsPrenoms, info.nomsPrenoms);
		assertEquals(nosAvs, info.nosAvs);
	}

	private static void assertInfo(long id, RoleData.TypeContribuable type, int ofsCommune, TypeAutoriteFiscale tafDomicile, Integer noOfsDomicile, String ide, String raisonSociale, FormeLegale formeLegale, RolePMData info) {
		assertInfo(id, type, ofsCommune, tafDomicile, noOfsDomicile, info);
		assertEquals(ide, info.noIDE);
		assertEquals(raisonSociale, info.raisonSociale);
		assertEquals(formeLegale, info.formeJuridique);
	}

	private static void assertIgnore(long id, RoleResults.RaisonIgnore raison, RoleResults.RoleIgnore ignore) {
		assertEquals(id, ignore.noContribuable);
		assertEquals(raison, ignore.raison);
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années
	 */
	private PersonnePhysique newCtbVaudoisOrdinaire(MockCommune commune) {
		final PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, commune);
		return paul;
	}

	/**
	 * @return un contribuable vaudois arrivé dans le canton en 2007
	 */
	private PersonnePhysique newCtbVaudoisOrdinaireDepuis2007(MockCommune commune) {
		final PersonnePhysique incognito = addNonHabitant("Incog", "Nito", null, null);
		addForPrincipal(incognito, date(2007, 4, 13), MotifFor.ARRIVEE_HC, commune);
		return incognito;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2007
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEn2007(MockCommune commune) {
		final PersonnePhysique raoul = addNonHabitant("Raoul", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(raoul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HC, commune);
		addForPrincipal(raoul, date(2007, 10, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		return raoul;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton au 31.12.2007
	 */
	private PersonnePhysique newCtbVaudoisPartiHorsCantonTrenteEtUnDecembre(MockCommune commune) {
		final PersonnePhysique raoul = addNonHabitant("Marie", "Coller", date(1965, 4, 13), Sexe.FEMININ);
		addForPrincipal(raoul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HC, commune);
		addForPrincipal(raoul, date(2008, 1, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		return raoul;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse au 31.12.2007
	 */
	private PersonnePhysique newCtbVaudoisPartiHorsSuisseTrenteEtUnDecembre(MockCommune commune) {
		final PersonnePhysique marie = addNonHabitant("Jean", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(marie, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HS, commune);
		addForPrincipal(marie, date(2008, 1, 1), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		return marie;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2007 mais qui a gardé un immeuble
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final PersonnePhysique louis = addNonHabitant("Louis", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(louis, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HC, communeResidence);
		addForPrincipal(louis, date(2007, 10, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		addForSecondaire(louis, date(2001, 3, 2), MotifFor.ACHAT_IMMOBILIER, communeImmeuble, MotifRattachement.IMMEUBLE_PRIVE);
		return louis;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse en 2007 mais qui a gardé un immeuble
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsSuisseEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final PersonnePhysique louis = addNonHabitant("Albert", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(louis, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HS, communeResidence);
		addForPrincipal(louis, date(2007, 10, 1), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		addForSecondaire(louis, date(2001, 3, 2), MotifFor.ACHAT_IMMOBILIER, communeImmeuble, MotifRattachement.IMMEUBLE_PRIVE);
		return louis;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2006
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEn2006(MockCommune commune) {
		final PersonnePhysique didier = addNonHabitant("Didier", "Duvolet", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(didier, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 9, 30), MotifFor.DEPART_HC, commune);
		addForPrincipal(didier, date(2006, 10, 1), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		return didier;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2008
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEn2008(MockCommune commune) {
		final PersonnePhysique laurent = addNonHabitant("Laurent", "Du", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(laurent, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2008, 9, 30), MotifFor.DEPART_HC, commune);
		addForPrincipal(laurent, date(2008, 10, 1), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		return laurent;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse et revenue en Suisse la même anneée en 2007
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune commune) {
		final PersonnePhysique benjamin = addNonHabitant("Benjamin", "TientPasEnPlace", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(benjamin, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, commune);
		addForPrincipal(benjamin, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne);
		addForPrincipal(benjamin, date(2007, 10, 1), MotifFor.ARRIVEE_HS, commune);
		return benjamin;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse et revenue en Suisse la même anneée en 2007
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune communeAvant, MockCommune communeApres) {
		final PersonnePhysique benjamin = addNonHabitant("Benjamin", "TientPasEnPlace", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(benjamin, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, communeAvant);
		addForPrincipal(benjamin, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne);
		addForPrincipal(benjamin, date(2007, 10, 1), MotifFor.ARRIVEE_HS, communeApres);
		return benjamin;
	}

	/**
	 * @return un contribuable sourcier parti hors-Suisse et revenue en Suisse la même anneée en 2007
	 */
	private PersonnePhysique newCtbVaudoisSourcierPartiHorsSuisseEtRevenuDansLaMemeAnnee(long noIndividu, MockCommune communeAvant, MockCommune communeApres) {
		final PersonnePhysique m = addHabitant(noIndividu);
		addForPrincipal(m, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, communeAvant, ModeImposition.SOURCE);
		addForPrincipal(m, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne, ModeImposition.SOURCE);
		addForPrincipal(m, date(2007, 10, 1), MotifFor.ARRIVEE_HS, communeApres, ModeImposition.SOURCE);
		return m;
	}

	/**
	 * @return un contribuable vaudois ordinaire avec for principal et secondaire dans la même commune
	 */
	private PersonnePhysique newCtbVaudoisOrdinaireAvecImmeubleDansCommune(MockCommune commune) {
		final PersonnePhysique genevieve = addNonHabitant("Geneviève", "Maillefer", date(1965, 4, 13), Sexe.FEMININ);
		addForPrincipal(genevieve, date(2003, 10, 1), MotifFor.DEMENAGEMENT_VD, commune);
		addImmeuble(genevieve, commune, date(2003, 11, 25), null);
		return genevieve;
	}

	/**
	 * @return un diplomate suisse basé à l'étranger mais rattaché à une commune vaudoise
	 */
	private PersonnePhysique newCtbDiplomateSuisse(MockCommune commune) {
		final PersonnePhysique marc = addNonHabitant("Marc", "Ramatruelle", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(marc, date(1968, 11, 3), MotifFor.MAJORITE, commune, MotifRattachement.DIPLOMATE_SUISSE);
		return marc;
	}

	private PersonnePhysique newCtbOrdinaireVaudoisEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final PersonnePhysique pascal = newCtbVaudoisOrdinaire(communeResidence);
		addImmeuble(pascal, communeImmeuble, date(2000, 1, 1), null);
		return pascal;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier-mixte
	 */
	private PersonnePhysique newCtbVaudoisSourcierMixte(MockCommune commune) {
		final PersonnePhysique arnold = addNonHabitant("Arnold", "Duplat", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(arnold, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune, ModeImposition.MIXTE_137_2);
		return arnold;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier pur (non gris!)
	 */
	private PersonnePhysique newCtbVaudoisSourcier(MockCommune commune) {
		final PersonnePhysique victor = addNonHabitant("Victor", "Duplat", date(1965, 4, 13), Sexe.MASCULIN);
		victor.setNumeroIndividu(263343L);
		addForPrincipal(victor, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune, ModeImposition.SOURCE);
		return victor;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier pur (gris)
	 */
	private PersonnePhysique newCtbVaudoisSourcierGris(MockCommune commune) {
		final PersonnePhysique albertine = addNonHabitant("Albertine", "Duplat", date(1969, 4, 13), Sexe.FEMININ);
		addForPrincipal(albertine, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune, ModeImposition.SOURCE);
		return albertine;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton
	 */
	private PersonnePhysique newCtbHorsCantonEtImmeuble(MockCommune commune) {
		final PersonnePhysique geo = addNonHabitant("Geo", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(geo, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(geo, commune, date(2003, 3, 1), null);
		return geo;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec deux immeubles dans le canton, dont un acheté et vendu en 2007
	 */
	private PersonnePhysique newCtbHorsCantonEtDeuxImmeubles(MockCommune communeImmeuble1, MockCommune communeImmeuble2) {
		final PersonnePhysique donald = addNonHabitant("Donald", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(donald, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(donald, communeImmeuble1, date(2007, 3, 1), date(2007, 6, 30));
		addImmeuble(donald, communeImmeuble2, date(1990, 1, 15), null);
		return donald;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec deux immeubles dans le canton, l'un vendu en 2007, l'autre acheté en 2007, sans chevauchement
	 */
	private PersonnePhysique newCtbHorsCantonDeuxImmeublesNonChevauchant(MockCommune communeImmeuble1, MockCommune communeImmeuble2) {
		final PersonnePhysique georges = addNonHabitant("Georges", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(georges, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(georges, communeImmeuble1, date(1980, 3, 1), date(2007, 6, 30));
		addImmeuble(georges, communeImmeuble2, date(2007, 11, 15), null);
		return georges;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton qui a été vendu en 2007
	 */
	private PersonnePhysique newCtbHorsCantonEtImmeubleVenduEn2007(MockCommune commune) {
		final PersonnePhysique johnny = addNonHabitant("Johnny", "Hallyday", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(johnny, date(2005, 11, 3), null, MockCommune.Bern);
		addImmeuble(johnny, commune, date(2005, 11, 3), date(2007, 8, 30));
		return johnny;
	}

	/**
	 * @return un contribuable avec un for principal hors Suisse, et avec un immeuble dans le canton qui a été vendu le 31.12.2007
	 */
	private PersonnePhysique newCtbHorsSuisseImmeubleVenduTrenteEtUnDecembre(MockCommune commune) {
		final PersonnePhysique tom = addNonHabitant("Tom", "Cruise", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tom, date(2005, 11, 3), null, MockPays.Albanie);
		addImmeuble(tom, commune, date(2005, 11, 3), date(2007, 12, 31));
		return tom;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec une activité indépendente dans le canton qui a été stoppé en 2007
	 */
	private PersonnePhysique newCtbHorsCantonEtActiviteIndStoppeeEn2007(MockCommune commune) {
		final PersonnePhysique tyler = addNonHabitant("Tyler", "Brulé", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tyler, date(2005, 11, 3), null, MockCommune.Bern);
		addForSecondaire(tyler, date(2005, 11, 3), MotifFor.DEBUT_EXPLOITATION, date(2007, 8, 30), MotifFor.FIN_EXPLOITATION, commune, MotifRattachement.ACTIVITE_INDEPENDANTE);
		return tyler;
	}

	/**
	 * @return un contribuable avec un for fermé en 1983
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEn1983(MockCommune commune) {
		final PersonnePhysique pierre = addNonHabitant("Pierre", "Dubateau", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(pierre, date(1968, 11, 3), MotifFor.ARRIVEE_HC, date(1983, 7, 1), MotifFor.DEPART_HC, commune);
		return pierre;
	}

	/**
	 * @return un contribuable avec un for annulé
	 */
	private PersonnePhysique newCtbVaudoisOrdinaireAnnule(MockCommune commune) {
		final PersonnePhysique jean = addNonHabitant("Jean", "Duchmol", date(1948, 11, 3), Sexe.MASCULIN);
		final ForFiscalPrincipal fors = addForPrincipal(jean, date(1968, 11, 3), MotifFor.ARRIVEE_HC, commune);
		fors.setAnnulationDate(DateHelper.getDate(1967, 1, 1));
		return jean;
	}

	/**
	 * @return un contribuable avec un for hors canton
	 */
	private PersonnePhysique newCtbHorsCantonSansForSecondaire() {
		final PersonnePhysique jeans = addNonHabitant("Jean", "Studer", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(jeans, date(1968, 11, 3), null, MockCommune.Neuchatel);
		return jeans;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton mais vendu en 2005
	 */
	private PersonnePhysique newCtbHorsCantonEtImmeubleVenduEn2005(MockCommune commune) {
		final PersonnePhysique popol = addNonHabitant("Popol", "Dillon", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(popol, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(popol, commune, date(2003, 3, 1), date(2005, 5, 31));
		return popol;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un for immeuble annulé
	 */
	private PersonnePhysique newCtbHorsCantonEtForImmeubleAnnule(MockCommune commune) {
		final PersonnePhysique rama = addNonHabitant("Rama", "Truelle", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(rama, date(1968, 11, 3), null, MockCommune.Neuchatel);
		ForFiscalSecondaire fs = addImmeuble(rama, commune, date(2003, 3, 1), null);
		fs.setAnnule(true);
		return rama;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois d'une commune à l'autre
	 */
	private PersonnePhysique newCtbOrdinaireAvecDemenagement(MockCommune avant, MockCommune apres) {
		final RegDate demenagement = date(2007, 6, 1);
		final PersonnePhysique ctb = addNonHabitant("Turlu", "Tutu", date(1947, 3, 25), Sexe.MASCULIN);
		addForPrincipal(ctb, date(1990, 2, 1), MotifFor.ARRIVEE_HS, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, avant);
		addForPrincipal(ctb, demenagement, MotifFor.DEMENAGEMENT_VD, apres);
		return ctb;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois qui garde un immeuble de part et d'autre du déménagement
	 */
	private PersonnePhysique newCtbOrdinaireAvecDemenagementEnGardantImmeuble(MockCommune avant, MockCommune apres, MockCommune communeImmeuble) {
		final PersonnePhysique ctb = newCtbOrdinaireAvecDemenagement(avant, apres);
		addForSecondaire(ctb, date(2005, 6, 12), MotifFor.ACHAT_IMMOBILIER, communeImmeuble, MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois d'une commune à l'autre (avant 2007)
	 */
	private PersonnePhysique newCtbOrdinaireAvecDemenagementAnterieur(MockCommune avant, MockCommune apres) {
		final RegDate demenagement = date(2005, 6, 1);
		final PersonnePhysique ctb = addNonHabitant("Turlu", "Tutu", date(1947, 3, 25), Sexe.MASCULIN);
		addForPrincipal(ctb, date(1990, 2, 1), MotifFor.ARRIVEE_HS, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, avant);
		addForPrincipal(ctb, demenagement, MotifFor.DEMENAGEMENT_VD, apres);
		return ctb;
	}

	/**
	 * @return un contribuable invalide
	 */
	private Long newCtbVaudoisOrdinaireEtImmeubleInvalide() throws Exception {
		return doInNewTransactionAndSessionWithoutValidation(status -> {
			PersonnePhysique pp = addNonHabitant("Rodolf", "Piedbor", date(1953, 12, 18), Sexe.MASCULIN);
			addForPrincipal(pp, date(1971, 12, 18), MotifFor.MAJORITE, MockCommune.Lausanne);
			// le for secondaire n'est pas couvert par le for principal
			addForSecondaire(pp, date(1920, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});
	}

	/**
	 * UNIREG-2777
	 * @return un contribuable HC avec deux fors secondaires immeubles ouverts sur l'OID d'Orbe et un autre fermé l'année des rôles sur l'OID de Lausanne
	 */
	private PersonnePhysique getCtbHorsCantonAvecDeuxForsImmeublesOuvertsPourJIRA2777() {
		final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
		addForPrincipal(pp, date(1988, 9, 12), MotifFor.MAJORITE, date(2007, 6, 11), MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(pp, date(2007, 6, 12), MotifFor.DEPART_HC, MockCommune.Bern);
		addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 15), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(pp, date(2003, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.RomainmotierEnvy, MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(pp, date(2004, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy, MotifRattachement.IMMEUBLE_PRIVE);
		return pp;
	}

	private ForFiscalSecondaire addImmeuble(final Contribuable ctb, MockCommune commune, RegDate debut, RegDate fin) {
		MotifFor motifFermeture = (fin == null ? null : MotifFor.VENTE_IMMOBILIER);
		return addForSecondaire(ctb, debut, MotifFor.ACHAT_IMMOBILIER, fin, motifFermeture, commune, MotifRattachement.IMMEUBLE_PRIVE);
	}

	private static <T> void assertNextIs(final Iterator<T> iter, T expected) {
		assertTrue(iter.hasNext());

		final T actual = iter.next();
		assertNotNull(actual);
		assertEquals(expected, actual);
	}

	@Test
	public void testSourcierGrisMarieDepartHS() throws Exception {

		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(1990, 4, 13), null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 9, 10), MotifFor.DEPART_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
			return mc.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2008, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(1, results.ignores.size());
		assertEquals(0, results.extraction.size());

		final RoleResults.RoleIgnore ignore = results.ignores.get(0);
		assertNotNull(ignore);
		assertIgnore(mcId, RoleResults.RaisonIgnore.SOURCIER_GRIS, ignore);
	}

	@Test
	public void testSourcierGrisCelibataireDepartHS() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
			addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 9, 10), MotifFor.DEPART_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2008, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(1, results.ignores.size());
		assertEquals(0, results.extraction.size());

		final RoleResults.RoleIgnore ignore = results.ignores.get(0);
		assertNotNull(ignore);
		assertIgnore(ppId, RoleResults.RaisonIgnore.SOURCIER_GRIS, ignore);
	}

	/**
	 * SIFISC-1797
	 */
	@Test
	public void testSourcierGrisCelibataireDepartHSAvecForHSRenseigne() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
			addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 9, 10), MotifFor.DEPART_HS, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			addForPrincipal(pp, date(2008, 9, 11), MotifFor.DEPART_HS, null, null, MockPays.PaysInconnu, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2008, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(1, results.ignores.size());
		assertEquals(0, results.extraction.size());

		final RoleResults.RoleIgnore ignore = results.ignores.get(0);
		assertNotNull(ignore);
		assertIgnore(ppId, RoleResults.RaisonIgnore.SOURCIER_GRIS, ignore);
	}

	/**
	 * Documente comment le passage d'une commune avant à après fusion agit sur l'édition des rôles de l'ancienne commune
	 */
	@Test
	public void testFusionDeCommunes() throws Exception {

		final long noIndividu = 324561L;
		final RegDate arrivee = date(2007, 7, 1);
		final MockCommune communeAvant = MockCommune.Villette;
		final MockCommune communeApres = MockCommune.BourgEnLavaux;

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1976, 3, 11), "Dantès", "Edmond", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Villette.CheminDeCreuxBechet, null, arrivee, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, communeAvant.getDateFinValidite(), MotifFor.FUSION_COMMUNES, communeAvant);
			addForPrincipal(pp, communeApres.getDateDebutValidite(), MotifFor.FUSION_COMMUNES, communeApres);
			return pp.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(communeAvant.getDateFinValidite().year(), 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		assertEquals(Collections.singleton(MockCommune.Villette.getNoOFS()), results.extraction.keySet());
		final List<RolePPData> infoCommune = results.extraction.get(MockCommune.Villette.getNoOFS());
		assertNotNull(infoCommune);
		assertEquals(1, infoCommune.size());
		final RolePPData data = infoCommune.get(0);
		assertNotNull(data);
		assertInfo(ppId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Villette.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Villette.getNoOFS(), Collections.singletonList(new NomPrenom("Dantès", "Edmond")), Collections.emptyList(), data);
	}

	/**
	 * SIFISC-8671
	 */
	@Test
	public void testSourcierPurVaudoisAvecForSecondaire() throws Exception {

		final long noIndividu = 32372567L;
		final RegDate dateArrivee = date(2000, 4, 1);
		final RegDate dateAchat = date(2010, 6, 12);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1976, 3, 11), "Dantès", "Edmond", true);
				addNationalite(ind, MockPays.France, date(1976, 3, 11), null);
				addPermis(ind, TypePermis.SEJOUR, dateArrivee, null, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2012, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		final List<RolePPData> infos = results.extraction.get(MockCommune.Bussigny.getNoOFS());
		assertNotNull(infos);
		assertEquals(1, infos.size());

		final RolePPData data = infos.get(0);
		assertNotNull(data);
		assertInfo(ppId, RoleData.TypeContribuable.SOURCE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), Collections.singletonList(new NomPrenom("Dantès", "Edmond")), Collections.emptyList(), data);
	}

	/**
	 * SIFISC-8671
	 */
	@Test
	public void testSourcierPurVaudoisAvecForSecondaireEtChangementModeImposition() throws Exception {

		final long noIndividu = 32372567L;
		final RegDate dateArrivee = date(2000, 4, 1);
		final RegDate dateAchat = date(2010, 6, 12);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1976, 3, 11), "Dantès", "Edmond", true);
				addNationalite(ind, MockPays.France, date(1976, 3, 11), null);
				addPermis(ind, TypePermis.SEJOUR, dateArrivee, null, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateAchat.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Bussigny, ModeImposition.SOURCE);
			addForPrincipal(pp, dateAchat, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Bussigny, ModeImposition.MIXTE_137_1);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(2012, 1, null, null);
		assertNotNull(results);
		assertEquals(1, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(0, results.ignores.size());
		assertEquals(1, results.extraction.size());

		final List<RolePPData> infos = results.extraction.get(MockCommune.Bussigny.getNoOFS());
		assertNotNull(infos);
		assertEquals(1, infos.size());

		final RolePPData data = infos.get(0);
		assertNotNull(data);
		assertInfo(ppId, RoleData.TypeContribuable.MIXTE, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bussigny.getNoOFS(), Collections.singletonList(new NomPrenom("Dantès", "Edmond")), Collections.emptyList(), data);
	}

	/**
	 * [SIFISC-11991] Trouvé deux cas en production lors du tir des rôles 2013, les deux sont des PP dont l'assujettissement est "Source Pure", qui
	 * se marient avec une personne pour constituer un couple à l'ordinaire dans l'année des rôles
	 * <p/>
	 * Cas 1 : cas de la personne qui était HC (source), arrive sur VD en début d'année (= ordinaire) et se marie, toujours la même année
	 */
	@Test
	public void testArriveeHCSourceVersOrdinairePuisMariageAnneeRole() throws Exception {

		final long noIndividuLui = 125626L;
		final long noIndividuElle = 4541515L;
		final int anneeRoles = 2013;
		final RegDate dateArrivee = date(anneeRoles, 1, 15);
		final RegDate dateMariage = date(anneeRoles, 5, 3);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Dupont", "Philippe", Sexe.MASCULIN);
				final MockIndividu elle = addIndividu(noIndividuElle, null, "Dupont", "Martine", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final class Ids {
			long ppId;
			long mcId;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(2000, 1, 1), MotifFor.DEPART_HC, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.SOURCE);
			addForPrincipal(lui, dateArrivee, MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.ORDINAIRE);

			final PersonnePhysique elle = addHabitant(noIndividuElle);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.ORDINAIRE);

			final Ids ids1 = new Ids();
			ids1.ppId = lui.getNumero();
			ids1.mcId = mc.getNumero();
			return ids1;
		});

		final RolePPCommunesResults results = roleService.produireRolePPCommunes(anneeRoles, 1, null, null);
		assertNotNull(results);
		assertEquals(2, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(1, results.ignores.size());
		assertEquals(1, results.extraction.size());

		final RoleResults.RoleIgnore ignore = results.ignores.get(0);
		assertNotNull(ignore);
		assertIgnore(ids.ppId, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, ignore);

		final List<RolePPData> infos = results.extraction.get(MockCommune.Leysin.getNoOFS());
		assertNotNull(infos);
		assertEquals(1, infos.size());

		final RolePPData data = infos.get(0);
		assertNotNull(data);
		assertInfo(ids.mcId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Leysin.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Leysin.getNoOFS(),
		           Arrays.asList(new NomPrenom("Dupont", "Philippe"), new NomPrenom("Dupont", "Martine")),
		           Arrays.asList(StringUtils.EMPTY, StringUtils.EMPTY),
		           data);
	}

	/**
	 * [SIFISC-11991] Trouvé deux cas en production lors du tir des rôles 2013, les deux sont des PP dont l'assujettissement est "Source Pure", qui
	 * se marient avec une personne pour constituer un couple à l'ordinaire dans l'année des rôles
	 * <p/>
	 * Cas 2 : mixte 1 avec immeuble (acheté en début d'année) qui se marie et forme un couple à l'ordinaire
	 */
	@Test
	public void testMixte1AchatImmeublePuisMariageAnneeRole() throws Exception {

		final long noIndividu = 125626L;
		final int anneeRoles = 2013;
		final RegDate dateChangementModeImposition = date(anneeRoles, 1, 1);
		final RegDate dateAchat = date(anneeRoles, 2, 15);
		final RegDate dateMariage = date(anneeRoles, 5, 3);
		final RegDate dateDemenagement = date(anneeRoles, 10, 6);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Dupont", "Philippe", Sexe.MASCULIN);
				marieIndividu(individu, dateMariage);
			}
		});

		final class Ids {
			long ppId;
			long mcId;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, dateChangementModeImposition.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Fraction.LeSentier, ModeImposition.SOURCE);
			addForPrincipal(pp, dateChangementModeImposition, MotifFor.CHGT_MODE_IMPOSITION, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LeSentier, ModeImposition.MIXTE_137_1);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye, MotifRattachement.IMMEUBLE_PRIVE);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Fraction.LeSentier, ModeImposition.ORDINAIRE);
			addForPrincipal(mc, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Fraction.LePont, ModeImposition.ORDINAIRE);
			addForSecondaire(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye, MotifRattachement.IMMEUBLE_PRIVE);

			final Ids ids1 = new Ids();
			ids1.ppId = pp.getNumero();
			ids1.mcId = mc.getNumero();
			return ids1;
		});

		// calcul des rôles
		final RolePPCommunesResults results = roleService.produireRolePPCommunes(anneeRoles, 1, null, null);
		assertNotNull(results);
		assertEquals(2, results.getNbContribuablesTraites());
		assertEquals(0, results.errors.size());
		assertEquals(1, results.ignores.size());        // la personne physique qui s'est mariée
		assertEquals(1, results.extraction.size());
		assertEquals(Collections.singleton(MockCommune.Fraction.LePont.getNoOFS()), results.extraction.keySet());

		// les ignorés
		{
			final RoleResults.RoleIgnore ignore = results.ignores.get(0);
			assertNotNull(ignore);
			assertIgnore(ids.ppId, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, ignore);
		}

		{
			// le pont
			final List<RolePPData> infoCommune = results.extraction.get(MockCommune.Fraction.LePont.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.size());

			final RolePPData data = infoCommune.get(0);
			assertNotNull(data);
			assertInfo(ids.mcId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Fraction.LePont.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Fraction.LePont.getNoOFS(),
			           Collections.singletonList(new NomPrenom("Dupont", "Philippe")), Collections.singletonList(StringUtils.EMPTY), data);
		}
	}

	/**
	 * [SIFISC-13803] NPE dans le calcul des rôles de l'OID si on avait, dans le même OID, un for fermé suivi par un for ouvert dans la liste
	 * présentée par {@link ch.vd.unireg.tiers.Tiers#getForsFiscauxValidAt(RegDate)}
	 */
	@Test
	public void testContribuableHorsSuisseAvecPlusieursImmeublesDansMemeOID() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				// personne...
			}
		});

		final RegDate dateAchat = date(2011, 6, 4);
		final RegDate dateVente1 = date(2013, 12, 11);
		final RegDate dateVente2 = date(2014, 3, 18);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alberto", "Tamburini", null, Sexe.MASCULIN);
			addForPrincipal(pp, dateAchat, null, MockPays.Allemagne);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente1, MotifFor.VENTE_IMMOBILIER, MockCommune.Renens, MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente2, MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.VufflensLaVille, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// rôles 2014 de l'OID de Lausanne
		final RolePPOfficesResults res = roleService.produireRolePPOffices(2013, 1, MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), null);
		assertNotNull(res);
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(Collections.singleton(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm()), res.extraction.keySet());

		final List<RolePPData> infoOid = res.extraction.get(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
		assertNotNull(infoOid);
		assertEquals(1, infoOid.size());
		final RolePPData data = infoOid.get(0);
		assertNotNull(data);
		assertInfo(ppId, RoleData.TypeContribuable.HORS_SUISSE, MockCommune.VufflensLaVille.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MockPays.Allemagne.getNoOFS(), Collections.singletonList(new NomPrenom("Tamburini", "Alberto")), Collections.emptyList(), data);
	}

	/**
	 * [SIFISC-23168] Cas du contribuable HS avec activité indépendante vaudoise qui se marie dans l'année des rôles
	 */
	@Test
	public void testHorsSuisseActiviteIndependanteQuiSeMarie() throws Exception {

		final int anneeRole = 2016;
		final RegDate dateMariage = date(anneeRole, 2, 17);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				// le contribuable est HS... rien de connu dans les contrôles des habitants vaudois
			}
		});

		final class Ids {
			long pp;
			long mc;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Tanguy", "Laverdure", date(1987, 5, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(2005, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.France);
			addForSecondaire(pp, date(2005, 1, 1), MotifFor.DEBUT_EXPLOITATION, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens, MotifRattachement.ACTIVITE_INDEPENDANTE);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.France);
			addForSecondaire(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens, MotifRattachement.ACTIVITE_INDEPENDANTE);

			final Ids result = new Ids();
			result.pp = pp.getNumero();
			result.mc = mc.getNumero();
			return result;
		});

		// calcul du rôle
		final RolePPCommunesResults res = roleService.produireRolePPCommunes(anneeRole, 1, null, null);
		assertNotNull(res);
		assertEquals(2, res.getNbContribuablesTraites());
		assertEquals(0, res.errors.size());
		assertEquals(1, res.ignores.size());        // la personne physique qui s'est mariée
		assertEquals(1, res.extraction.size());     // le ménage commun
		assertEquals(Collections.singleton(MockCommune.Echallens.getNoOFS()), res.extraction.keySet());

		// le détail

		{
			final RoleResults.RoleIgnore ignore = res.ignores.get(0);
			assertNotNull(ignore);
			assertIgnore(ids.pp, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, ignore);
		}

		{
			// Echallens
			final List<RolePPData> infoCommune = res.extraction.get(MockCommune.Echallens.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.size());

			final RolePPData data = infoCommune.get(0);
			assertNotNull(data);
			assertInfo(ids.mc, RoleData.TypeContribuable.HORS_SUISSE, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MockPays.France.getNoOFS(), Collections.singletonList(new NomPrenom("Laverdure", "Tanguy")), Collections.singletonList(StringUtils.EMPTY), data);
		}
	}

	@Test
	public void testRolesPMSimple() throws Exception {

		// entreprise HC qui possède un immeuble dans une commune vaudoise
		final RegDate dateDebut = date(2010, 5, 6);
		final RegDate dateAchat = date(2012, 8, 14);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
			addForPrincipal(pm, dateAchat, null, MockCommune.Bern);
			addForSecondaire(pm, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Grandson, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(2015, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(Collections.singleton(MockCommune.Grandson.getNoOFS()), res.extraction.keySet());      // juste Grandson

		{
			final List<RolePMData> infoCommune = res.extraction.get(MockCommune.Grandson.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.size());

			final RolePMData info = infoCommune.get(0);
			assertNotNull(info);
			assertInfo(pmId, RoleData.TypeContribuable.HORS_CANTON, MockCommune.Grandson.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Bern.getNoOFS(), null, "Raison d'un jour dure toujours", FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info);
		}
	}

	@Test
	public void testRolesPMVenteAvantFinExercice() throws Exception {

		// entreprise HC qui possède un immeuble dans une commune vaudoise
		final RegDate dateDebut = date(2010, 5, 6);
		final RegDate dateAchat = date(2012, 8, 14);
		final RegDate dateVente = date(2015, 2, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
			addForPrincipal(pm, dateAchat, null, MockCommune.Bern);
			addForSecondaire(pm, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Grandson, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(2015, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(1, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(0, res.extraction.size());
		assertIgnore(pmId, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, res.ignores.get(0));
	}

	/**
	 * Au début, on pensait que pour les entreprises vaudoises, les fors secondaires ne devait pas intervenir...
	 * Et puis on est revenu sur cette décision (mail de PTF, 07.09.2016), donc maintenant, c
	 */
	@Test
	public void testRolesPMEntrepriseVaudoiseAvecForSecondaires() throws Exception {

		final RegDate dateDebut = date(2010, 5, 6);
		final RegDate dateAchat = date(2012, 8, 14);
		final RegDate dateVente = date(2015, 2, 4);
		final String ide = "CHE213456789";

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addIdentificationEntreprise(pm, ide);
			addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
			addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addForSecondaire(pm, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Grandson, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(2015, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(Collections.singleton(MockCommune.Lausanne.getNoOFS()), res.extraction.keySet());

		{
			final List<RolePMData> infoCommune = res.extraction.get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.size());
			final RolePMData data = infoCommune.get(0);
			assertNotNull(data);
			assertInfo(pmId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), ide, "Raison d'un jour dure toujours", FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, data);
		}
	}

	/**
	 * Au début, on pensait que pour les entreprises vaudoises, les fors secondaires ne devait pas intervenir...
	 * Et puis on est revenu sur cette décision (mail de PTF, 07.09.2016), donc maintenant, c
	 */
	@Test
	public void testRolesOIPMEntrepriseVaudoiseAvecForSecondaires() throws Exception {

		final RegDate dateDebut = date(2010, 5, 6);
		final RegDate dateAchat = date(2012, 8, 14);
		final RegDate dateVente = date(2015, 2, 4);
		final String ide = "CHE213456789";

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addIdentificationEntreprise(pm, ide);
			addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
			addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addForSecondaire(pm, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Grandson, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMOfficeResults res = roleService.produireRolePMOffice(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(1, res.extraction.size());

		{
			final RolePMData data = res.extraction.get(0);
			assertNotNull(data);
			assertInfo(pmId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), ide, "Raison d'un jour dure toujours", FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, data);
		}
	}

	@Test
	public void testRolesPMDeuxBouclementsDansAnnee() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(pm, dateDebut, DayMonth.get(3, 31), 12);              // bouclements tous les 31.03 depuis 2011 jusqu'à 2015
			addBouclement(pm, date(2015, 12, 1), DayMonth.get(12, 31), 12);     // dès 2015, bouclements au 31.12 -> 2 bouclements en 2015
			addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(2015, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(1, res.extraction.size());      // juste Lausanne...

		{
			final List<RolePMData> infoCommune = res.extraction.get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.size());        // même avec plusieurs bouclements, l'entreprise n'est présente qu'à un endroit à la fin de l'année

			final RolePMData info = infoCommune.get(0);
			assertNotNull(info);
			assertInfo(pmId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, "Raison d'un jour dure toujours", FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info);
		}
	}

	@Test
	public void testRolesOIPMDeuxBouclementsDansAnnee() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(pm, dateDebut, DayMonth.get(3, 31), 12);              // bouclements tous les 31.03 depuis 2011 jusqu'à 2015
			addBouclement(pm, date(2015, 12, 1), DayMonth.get(12, 31), 12);     // dès 2015, bouclements au 31.12 -> 2 bouclements en 2015
			addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMOfficeResults res = roleService.produireRolePMOffice(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(1, res.extraction.size());

		final RolePMData data = res.extraction.get(0);
		assertNotNull(data);
		assertInfo(pmId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, "Raison d'un jour dure toujours", FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, data);
	}

	@Test
	public void testRolesPMDeuxBouclementsDansAnneeAvecDemenagementEntreDeux() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);
		final int anneeRoles = 2015;
		final RegDate dateDemenagement = date(anneeRoles, 7, 18);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(pm, dateDebut, DayMonth.get(3, 31), 12);                    // bouclements tous les 31.03 depuis 2011 jusqu'à 2015
			addBouclement(pm, date(anneeRoles, 12, 1), DayMonth.get(12, 31), 12);     // dès 2015, bouclements au 31.12 -> 2 bouclements en 2015
			addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
			addForPrincipal(pm, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Morges);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(anneeRoles, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(Collections.singleton(MockCommune.Morges.getNoOFS()), res.extraction.keySet());

		{
			final List<RolePMData> infoCommune = res.extraction.get(MockCommune.Morges.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.size());
			final RolePMData data = infoCommune.get(0);
			assertNotNull(data);
			assertInfo(pmId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Morges.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS(), null, "Raison d'un jour dure toujours", FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, data);
		}
	}

	@Test
	public void testRolesOIPMDeuxBouclementsDansAnneeAvecDemenagementEntreDeux() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);
		final int anneeRoles = 2015;
		final RegDate dateDemenagement = date(anneeRoles, 7, 18);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(pm, dateDebut, DayMonth.get(3, 31), 12);                    // bouclements tous les 31.03 depuis 2011 jusqu'à 2015
			addBouclement(pm, date(anneeRoles, 12, 1), DayMonth.get(12, 31), 12);     // dès 2015, bouclements au 31.12 -> 2 bouclements en 2015
			addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
			addForPrincipal(pm, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Morges);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(anneeRoles, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(Collections.singleton(MockCommune.Morges.getNoOFS()), res.extraction.keySet());

		final List<RolePMData> infoCommune = res.extraction.get(MockCommune.Morges.getNoOFS());
		assertNotNull(infoCommune);
		assertEquals(1, infoCommune.size());
		final RolePMData data = infoCommune.get(0);
		assertNotNull(data);
		assertInfo(pmId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Morges.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS(), null, "Raison d'un jour dure toujours", FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, data);
	}

	@Test
	public void testDepartHCPM() throws Exception {
		final RegDate dateDebut = date(2010, 5, 10);
		final int anneeRoles = 2015;
		final RegDate dateDemenagement = date(anneeRoles, 7, 18);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
			addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(pm, dateDebut, DayMonth.get(12, 31), 12);                    // bouclements tous les 31.12 depuis 2010
			addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDemenagement.getOneDayBefore(), MotifFor.DEPART_HC, MockCommune.Lausanne);
			addForPrincipal(pm, dateDemenagement, MotifFor.DEPART_HC, MockCommune.Neuchatel);
			return pm.getNumero();
		});

		// rôles 2015 vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(anneeRoles, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(1, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(0, res.extraction.size());

		final RoleResults.RoleIgnore ignore = res.ignores.get(0);
		assertNotNull(ignore);
		assertIgnore(pmId, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE, ignore);
	}

	/**
	 * [SIFISC-23155] La DGF aimerait qu'une société dont le for principal est fermé pour motif FAILLITE, dont l'assujettissement
	 * se poursuit jusqu'à la fin de l'année, soit présente dans le rôle de sa commune de for pour l'année...
	 */
	@Test
	public void testFailliteEtBouclementFinAnneeRole() throws Exception {

		final int anneeRole = 2016;
		final RegDate dateDebut = date(2009, 1, 5);
		final RegDate dateFaillite = date(anneeRole, 8, 12);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// rien du tout, c'est plus simple
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Aigle);
			return entreprise.getNumero();
		});

		// vérification de la date de fin d'assujettissement
		doInNewTransactionAndSession(transactionStatus -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			assertNotNull(entreprise);

			final List<Assujettissement> assujettissements = assujettissementService.determine(entreprise);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());

			final Assujettissement assujettissement = assujettissements.get(0);
			assertNotNull(assujettissement);
			assertEquals(date(anneeRole, 12, 31), assujettissement.getDateFin());
			return null;
		});

		// rôle vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(anneeRole, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(1, res.extraction.size());

		final List<RolePMData> dataAigle = res.extraction.get(MockCommune.Aigle.getNoOFS());
		assertNotNull(dataAigle);
		assertEquals(1, dataAigle.size());

		final RolePMData data = dataAigle.get(0);
		assertInfo(pmId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aigle.getNoOFS(), null, "Tralala SA", FormeLegale.N_0106_SOCIETE_ANONYME, data);
	}

	/**
	 * [SIFISC-23155] La DGF aimerait qu'une société dont le for principal est fermé pour motif FAILLITE, dont l'assujettissement
	 * ne se pas poursuit jusqu'à la fin de l'année, soit présente dans le rôle de sa commune de for pour l'année...
	 */
	@Test
	public void testFailliteEtBouclementAvantFinAnneeRole() throws Exception {

		final int anneeRole = 2016;
		final RegDate dateDebut = date(2009, 1, 5);
		final RegDate dateFaillite = date(anneeRole, 8, 12);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// rien du tout, c'est plus simple
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(11, 30), 12);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Aigle);
			return entreprise.getNumero();
		});

		// vérification de la date de fin d'assujettissement
		doInNewTransactionAndSession(transactionStatus -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			assertNotNull(entreprise);

			final List<Assujettissement> assujettissements = assujettissementService.determine(entreprise);
			assertNotNull(assujettissements);
			assertEquals(1, assujettissements.size());

			final Assujettissement assujettissement = assujettissements.get(0);
			assertNotNull(assujettissement);
			assertEquals(date(anneeRole, 11, 30), assujettissement.getDateFin());
			return null;
		});

		// rôle vaudois
		final RolePMCommunesResults res = roleService.produireRolePMCommunes(anneeRole, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(1, res.extraction.size());

		final List<RolePMData> dataAigle = res.extraction.get(MockCommune.Aigle.getNoOFS());
		assertNotNull(dataAigle);
		assertEquals(1, dataAigle.size());

		final RolePMData data = dataAigle.get(0);
		assertInfo(pmId, RoleData.TypeContribuable.ORDINAIRE, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aigle.getNoOFS(), null, "Tralala SA", FormeLegale.N_0106_SOCIETE_ANONYME, data);
	}

	/**
	 * [SIFISC-23383] Départ HS d'un sourcier mixte dans l'année du rôle PP
	 */
	@Test
	public void testDepartHSPourSourcierMixteDansAnneeRole() throws Exception {

		final int anneeRole = 2016;
		final RegDate dateDebut = date(2000, 5, 1);
		final RegDate dateDepartHS = date(anneeRole, 5, 12);
		final long noIndividu = 34284226L;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1954, 6, 21);
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Blum", "Katharina", Sexe.FEMININ);
				addNationalite(individu, MockPays.Allemagne, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateDebut, dateDepartHS);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, dateDebut, MotifFor.INDETERMINE, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Cossonay, ModeImposition.MIXTE_137_2);
			addForPrincipal(pp, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		// rôle vaudois
		final RolePPCommunesResults res = roleService.produireRolePPCommunes(anneeRole, 1, null, null);
		assertNotNull(res);
		assertEquals(0, res.errors.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.getNbContribuablesTraites());
		assertEquals(1, res.extraction.size());

		final List<RolePPData> dataCossonay = res.extraction.get(MockCommune.Cossonay.getNoOFS());
		assertNotNull(dataCossonay);
		assertEquals(1, dataCossonay.size());

		final RolePPData data = dataCossonay.get(0);
		assertInfo(ppId, RoleData.TypeContribuable.MIXTE, MockCommune.Cossonay.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MockPays.Allemagne.getNoOFS(), Collections.singletonList(new NomPrenom("Blum", "Katharina")), Collections.emptyList(), data);
	}
}
