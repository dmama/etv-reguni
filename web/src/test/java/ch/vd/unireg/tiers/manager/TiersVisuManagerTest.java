package ch.vd.unireg.tiers.manager;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.HistoFlag;
import ch.vd.unireg.tiers.HistoFlags;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.view.AdresseCivilView;
import ch.vd.unireg.tiers.view.AdresseView;
import ch.vd.unireg.tiers.view.TiersVisuView;
import ch.vd.unireg.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TiersVisuManagerTest extends WebTest {

	private TiersVisuManager tiersVisuManager;

	private static final String DB_UNIT_FILE = "TiersVisuManagerTest.xml";

	/**
	 * @see ch.vd.unireg.common.AbstractCoreDAOTest#onSetUp()
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addIndividu(282312, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);

			}
		});

		loadDatabase(DB_UNIT_FILE);
		tiersVisuManager = getBean(TiersVisuManager.class, "tiersVisuManager");

	}

	private static HistoFlags buildHistoFlags(boolean adressesHisto, boolean adressesHistoCiviles, boolean adressesHistoCivilesConjoint,
	                                          boolean raisonsSocialesCivileHistoParam, boolean nomsAdditionnelsHistoParam, boolean siegesCivilHistoParam, boolean formesJuridiquesCivileHistoParam, boolean capitauxCivileHistoParam, boolean domicilesHistoParam,
	                                          boolean rapportsPrestationHisto, boolean ctbAssocieHisto) {

		final Map<HistoFlag, Boolean> map = new EnumMap<>(HistoFlag.class);
		map.put(HistoFlag.ADRESSES, adressesHisto);
		map.put(HistoFlag.ADRESSES_CIVILES, adressesHistoCiviles);
		map.put(HistoFlag.ADRESSES_CIVILES_CONJOINT, adressesHistoCivilesConjoint);
		map.put(HistoFlag.RAISONS_SOCIALES, raisonsSocialesCivileHistoParam);
		map.put(HistoFlag.NOMS_ADDITIONNELS, nomsAdditionnelsHistoParam);
		map.put(HistoFlag.SIEGES, siegesCivilHistoParam);
		map.put(HistoFlag.FORMES_JURIDIQUES, formesJuridiquesCivileHistoParam);
		map.put(HistoFlag.CAPITAUX, capitauxCivileHistoParam);
		map.put(HistoFlag.DOMICILES, domicilesHistoParam);
		map.put(HistoFlag.RAPPORTS_PRESTATION, rapportsPrestationHisto);
		map.put(HistoFlag.CTB_ASSOCIE, ctbAssocieHisto);

		final Set<HistoFlag> raisedFlags = map.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(HistoFlag.class)));
		return new HistoFlags(raisedFlags);
	}

	/**
	 * Teste la methode getView
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetViewHabitant() throws Exception{

		final WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
		final TiersVisuView view = tiersVisuManager.getView((long) 6789,
		                                                    buildHistoFlags(true, true, true, true, true, true, true,true, true, false, false),
		                                                    true, true, true, true, webParamPagination);
		final Tiers tiers = view.getTiers();
		final PersonnePhysique hab = (PersonnePhysique) tiers;
		assertNotNull(hab);
		assertEquals("Bolomey", view.getIndividu().getNom());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetViewNonHabitant() throws Exception {
		final WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
		final TiersVisuView view = tiersVisuManager.getView((long) 12600002,
		                                                    buildHistoFlags(true, true, true, true, true, true, true, true,true, false,false),
		                                                    true, true, true, true, webParamPagination);
		final Tiers tiers = view.getTiers();
		final PersonnePhysique nonHab = (PersonnePhysique) tiers;
		assertNotNull(nonHab);
		assertEquals("Kamel", nonHab.getNom());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAdressesHistoriques() throws Exception {
		final WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
		final TiersVisuView view = tiersVisuManager.getView((long) 6789,
		                                              buildHistoFlags(true, true, true, true, true, true, true, true,true, false,false),
		                                              true, true, true, true, webParamPagination);
		final List<AdresseView> adresses = view.getHistoriqueAdresses();
		/*
		 * 2 * courrier
		 * 2 * representation (1 fiscale + 1 défaut)
		 * 2 * poursuite (1 défaut)
		 */
		assertEquals(6, adresses.size());

	}

	@Test
	public void testGetAdressesCivilesPrincipalHC() throws Exception {

	    serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu zotan = addIndividu(185386, RegDate.get(1974, 3, 22), "Zotan", "Mitev", true);
				MockIndividu marie = addIndividu(185387, RegDate.get(1974, 3, 22), "Marie-Claude", "Wolf", false);
				addAdresse(marie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(2009, 9, 1), null);
				addAdresse(marie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(2009, 9, 1), null);
				addAdresse(zotan, TypeAdresseCivil.COURRIER, null, null,null,null,null, MockPays.PaysInconnu, RegDate.get(2009, 12, 18), null);
				addAdresse(zotan, TypeAdresseCivil.PRINCIPALE, null, null,null,null,null, MockPays.PaysInconnu, RegDate.get(2009, 12, 18), null);
			   	marieIndividus(zotan, marie, date(2004, 5, 1));
			}
		});

		final class Numeros {
			long numeroContribuablePrincipal;
			long numeroContribuableConjoint;
			long numeroContribuableMenage;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitantZotan=	addHabitant(185386);
				PersonnePhysique habitantMarie=	addHabitant(185387);
				// Crée le ménage
				MenageCommun menage = new MenageCommun();
				RapportEntreTiers rapport = tiersService.addTiersToCouple(menage, habitantZotan, date(2004, 5, 1), null);
				menage = (MenageCommun) tiersDAO.get(rapport.getObjetId());
				numeros.numeroContribuableMenage = menage.getNumero();
				numeros.numeroContribuablePrincipal = habitantZotan.getNumero();

				rapport = tiersService.addTiersToCouple(menage, habitantMarie, date(2004, 5, 1), null);

				numeros.numeroContribuableConjoint = habitantMarie.getNumero();

				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
				final TiersVisuView view = tiersVisuManager.getView(numeros.numeroContribuableMenage,
				                                                    buildHistoFlags(true, true, true, true, true, true, true, true, true, false, false),
				                                                    true, true, true, true, webParamPagination);
				final List<AdresseView> adressesMenage = view.getHistoriqueAdresses();
				final List<AdresseCivilView> adressesZotan = view.getHistoriqueAdressesCiviles();
				final List<AdresseCivilView> adressesMarie = view.getHistoriqueAdressesCivilesConjoint();

				// 2 * courrier
				// 2 * representation (1 fiscale + 1 défaut)
				// 2 * poursuite (1 défaut)
				assertEquals(2, adressesZotan.size());
				assertEquals(date(2009, 12, 18), adressesZotan.get(0).getDateDebut());
				assertNull(adressesZotan.get(0).getLocalite());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-24996] Ce test vérifie que toutes les adresses civiles des entreprises sont bien affichées (notamment les adresses 'case postale').
	 */
	@Test
	public void testGetAdresseCivilesEntreprise() throws Exception {

		// on insère les données du Tennis-Club de Forel-Savigny (qui possède une boîte postale)
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.TENNIS_CLUB_FOREL_SAVIGNY);
			}
		});

		final Long id = doInNewTransaction(status -> {
			final Entreprise tennisClub = addEntrepriseConnueAuCivil(101830038L);
			return tennisClub.getId();
		});

		// on construit la vue 'web' de l'entreprise
		final TiersVisuView view = doInNewTransaction(new TxCallback<TiersVisuView>() {
			@Override
			public TiersVisuView execute(TransactionStatus status) throws Exception {
				final WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
				final HistoFlags flags = buildHistoFlags(true, true, false, false, false, false, false, false, false, false, false);
				return tiersVisuManager.getView(id, flags, false, false, false, false, webParamPagination);
			}
		});
		assertNotNull(view);

		// on vérifie que les adresses sont bien exposées
		final List<AdresseCivilView> histo = view.getHistoriqueAdressesCiviles();
		assertNotNull(histo);
		assertEquals(3, histo.size());
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2017, 8, 23), null, "Route de Vevey", "1072 Forel (Lavaux)", histo.get(0));
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2016, 9, 21), RegDate.get(2017, 8, 22), null, "1073 Savigny", histo.get(1));
		assertAdresse(TypeAdresseCivil.CASE_POSTALE, RegDate.get(2016, 12, 15), null, null, "1073 Savigny", histo.get(2));
		assertEquals("Case Postale 38", histo.get(2).getCasePostale());
	}

	/**
	 * [SIFISC-28037] Ce test vérifie que toutes les adresses civiles des établissements sont bien affichées (notamment les adresses 'case postale').
	 */
	@Test
	public void testGetAdresseCivilesEtablissement() throws Exception {

		final long numeroEtablissement = MockOrganisationFactory.TENNIS_CLUB_FOREL_SAVIGNY.getEtablissementPrincipal(null).getPayload().getNumeroEtablissement();

		// on insère les données du Tennis-Club de Forel-Savigny (dont l'établissement principal possède une boîte postale)
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.TENNIS_CLUB_FOREL_SAVIGNY);
			}
		});

		final Long id = doInNewTransaction(status -> {
			final Etablissement etablissement = addEtablissementConnuAuCivil(numeroEtablissement);
			return etablissement.getId();
		});

		// on construit la vue 'web' de l'entreprise
		final TiersVisuView view = doInNewTransaction(new TxCallback<TiersVisuView>() {
			@Override
			public TiersVisuView execute(TransactionStatus status) throws Exception {
				final WebParamPagination webParamPagination = new WebParamPagination(1, 10, "logCreationDate", true);
				final HistoFlags flags = buildHistoFlags(true, true, false, false, false, false, false, false, false, false, false);
				return tiersVisuManager.getView(id, flags, false, false, false, false, webParamPagination);
			}
		});
		assertNotNull(view);

		// on vérifie que les adresses sont bien exposées
		final List<AdresseCivilView> histo = view.getHistoriqueAdressesCiviles();
		assertNotNull(histo);
		assertEquals(3, histo.size());
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2017, 8, 23), null, "Route de Vevey", "1072 Forel (Lavaux)", histo.get(0));
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2016, 9, 21), RegDate.get(2017, 8, 22), null, "1073 Savigny", histo.get(1));
		assertAdresse(TypeAdresseCivil.CASE_POSTALE, RegDate.get(2016, 12, 15), null, null, "1073 Savigny", histo.get(2));
		assertEquals("Case Postale 38", histo.get(2).getCasePostale());
	}

	public TiersVisuManager getTiersVisuManager() {
		return tiersVisuManager;
	}

	public void setTiersVisuManager(TiersVisuManager tiersVisuManager) {
		this.tiersVisuManager = tiersVisuManager;
	}

	public static void assertAdresse(@NotNull TypeAdresseCivil type, @Nullable RegDate dateDebut, @Nullable RegDate dateFin, @Nullable String rue, @Nullable String localite, AdresseCivilView adresse) {
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertEquals(dateFin, adresse.getDateFin());
		assertEquals(rue, adresse.getRue());
		assertEquals(localite, adresse.getLocalite());
		assertEquals(type, adresse.getUsageCivil());
	}
}
