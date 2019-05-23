package ch.vd.unireg.interfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.evd0007.v1.Country;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.webservice.fidor.v5.FidorClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServiceInfrastructureServiceTest extends BusinessItTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfrastructureServiceTest.class);

	private ServiceInfrastructureService service;

	public ServiceInfrastructureServiceTest() {
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
	}

	@Test
	public void testSuisse() throws Exception {
		final Pays suisse = service.getSuisse();
		assertNotNull(suisse);
		assertEquals("Suisse", suisse.getNomCourt());
		assertEquals("Confédération suisse", suisse.getNomOfficiel());
		assertEquals("CH", suisse.getSigleOFS());
	}

	@Test
	public void testCantons() throws Exception {
		assertEquals(26, service.getAllCantons().size());
	}

	public void testCommunesDeVaud() throws Exception {
		assertEquals(396, service.getCommunesDeVaud().size());
	}

	public void testCantonDeZurich() throws Exception {
		Canton canton = service.getCantonBySigle("ZH");
		assertNotNull(canton);
		assertEquals("ZH", canton.getSigleOFS());
	}

	public void testCantonInexistant() throws Exception {
		try {
			// Should throw an exception
			service.getCanton(746238868);
			fail();
		}
		catch (Exception e) {
			// Ok
		}
	}

	public void testLocalites() throws Exception {

		List<Localite> localites = service.getLocalites();
		// Le nombre de localités varie de 4868 à 4885 ..., VA SAVOIR POURQUOI!!!
		int n = localites.size();
		LOGGER.info("Nombre de localités trouvées: " + n);
		boolean ok = n >= 4868 && n <= 4885;
		assertTrue(ok);
	}

	/**
	 * Teste d'un bug particulièrement coquin aperçu en développement : Lausanne n'était pas considéré (ni d'ailleurs aucune autre commune
	 * vaudoise) comme étant dans le canton !
	 * <p>
	 * Le problème, c'est que la méthode getAllCantons() retourne à chaque fois une liste contenant de nouvelles instances des cantons.
	 * Ensuite, on a:
	 * <ul>
	 * <li>getVaud() => getAllCantons() => instance n°1 du canton de Vaud crée et cachée</li>
	 * <li>estDansLeCanton() => getCantonBySigle() => getAllCantons => instance n°2 du canton de Vaud crée</li>
	 * </ul>
	 *
	 * ...ensuite les deux instances du cantons sont comparées avec equals() qui n'était <b>pas</b> défini sur les sous-classe de Canton =>
	 * false !
	 *
	 * La solution a été de forcer la définition de la méthode equals() sur toutes les EntityOFS.
	 */
	@Test
	public void testCommuneEstDansLeCanton() throws Exception {

		final Canton vaud = service.getVaud();
		assertNotNull(vaud);
		assertEquals(22, vaud.getNoOFS());

		final Commune lausanne = service.getCommuneByNumeroOfs(5586, null);
		assertNotNull(lausanne);
		assertEquals("Lausanne", lausanne.getNomOfficiel());

		assertTrue(service.estDansLeCanton(lausanne));
	}

	@Test
	public void testGetPaysByCode() throws Exception {

		final Pays ch = service.getPays("CH", null);
		assertEquals("CH", ch.getSigleOFS());
		assertEquals("Suisse", ch.getNomCourt());

		final Pays fr = service.getPays("FR", null);
		assertEquals("FR", fr.getSigleOFS());
		assertEquals("France", fr.getNomCourt());

		final Pays be = service.getPays("BE", null);
		assertEquals("BE", be.getSigleOFS());
		assertEquals("Belgique", be.getNomCourt());
	}

	@Test
	public void testDateValiditeCommuneLusseryVillars() throws Exception {

		final int noOfsLussery = 5487;
		{
			final Commune lussery = service.getCommuneByNumeroOfs(noOfsLussery, RegDate.get(1998, 12, 1));    // fusion au 31.12.1998
			assertNotNull(lussery);
			assertEquals("Lussery", lussery.getNomOfficiel());
		}
		{
			final Commune lusseryVillars = service.getCommuneByNumeroOfs(noOfsLussery, RegDate.get(1999, 1, 1));  // fusion au 31.12.1998
			assertNotNull(lusseryVillars);
			assertEquals("Lussery-Villars", lusseryVillars.getNomOfficiel());
		}
		{
			final Commune lusseryVillars = service.getCommuneByNumeroOfs(noOfsLussery, null);          // commune toujours ouverte
			assertNotNull(lusseryVillars);
			assertEquals("Lussery-Villars", lusseryVillars.getNomOfficiel());
		}
	}

	@Test
	public void testDateValiditeCommuneCloturee() throws Exception {

		final int noOfsHerlisberg = 1029;       // commune clôturée le 31.12.2004
		{
			final Commune herlisberg = service.getCommuneByNumeroOfs(noOfsHerlisberg, RegDate.get(2000, 1, 1));
			assertNotNull(herlisberg);      // la date ne devrait pas être prise en compte puisqu'il n'y a qu'une seule commune
			assertEquals(RegDate.get(2004, 12, 31), herlisberg.getDateFinValidite());
		}
		{
			final Commune herlisberg = service.getCommuneByNumeroOfs(noOfsHerlisberg, RegDate.get(2005, 1, 1));
			assertNotNull(herlisberg);      // la date ne devrait pas être prise en compte puisqu'il n'y a qu'une seule commune
			assertEquals(RegDate.get(2004, 12, 31), herlisberg.getDateFinValidite());
		}
		{
			final Commune herlisberg = service.getCommuneByNumeroOfs(noOfsHerlisberg, null);
			assertNotNull(herlisberg);      // la date ne devrait pas être prise en compte puisqu'il n'y a qu'une seule commune
			assertEquals(RegDate.get(2004, 12, 31), herlisberg.getDateFinValidite());
		}
	}

	@Test
	public void testGetCommuneByEgidCommuneFusionneeAuCivilMaisPasAuFiscal() throws Exception {

		// route de la Corniche 9bis, 1097 Riex (http://www.geoplanet.vd.ch/index.php?reset_session&linkit=1&switch_id=switch_localisation&layer_select=complement_vd2,fond_continu_gris,canton_select,adresses_select,cad_bat_hs_fond_select,npcs_bat_hs_select&recenter_bbox=545951.2,149324.8,546086.53,149426.27&mapsize=3&query_blocks[adresses_select]=129374357&query_hilight=1&query_return_attributes=1)
		final int immeuble = 280011227;

		// avant fusion civile/fiscale
		{
			final Commune commune = service.getCommuneByEgid(immeuble, date(2011, 4, 1));
			assertNotNull(commune);
			assertEquals(MockCommune.Riex.getNoOFS(), commune.getNoOFS());
			assertEquals("Riex", commune.getNomOfficiel());
		}

		// après fusion civile MAIS avant fusion fiscale
		{
			final Commune commune = service.getCommuneByEgid(immeuble, date(2011, 10, 1));
			assertNotNull(commune);
			assertEquals(MockCommune.Riex.getNoOFS(), commune.getNoOFS());
			assertEquals("Riex", commune.getNomOfficiel());
		}

		// après fusion civile ET après fusion fiscale
		{
			final Commune commune = service.getCommuneByEgid(immeuble, date(2012, 1, 1));
			assertNotNull(commune);
			assertEquals(MockCommune.BourgEnLavaux.getNoOFS(), commune.getNoOFS());
			assertEquals("Bourg-en-Lavaux", commune.getNomOfficiel());
		}
	}

	@Test
	public void testGetCommuneHistoByNumeroOFS() throws Exception {

		final List<Commune> list = service.getCommuneHistoByNumeroOfs(MockCommune.BourgEnLavaux.getNoOFS());
		assertNotNull(list);
		assertEquals(1, list.size());

		final Commune commune = list.get(0);
		assertEquals(MockCommune.BourgEnLavaux.getNoOFS(), commune.getNoOFS());
		assertEquals("Bourg-en-Lavaux", commune.getNomOfficiel());
		assertEquals(RegDate.get(2012, 1, 1), commune.getDateDebutValidite());
	}

	/**
	 * [SIFISC-6936] Vérifie que Fidor est bien capable de nous retourner une seule commune pour cet egid.
	 */
	@Test
	public void testGetCommuneParEgid() throws Exception {
		final Commune commune = service.getCommuneByEgid(280081618, date(2012, 11, 2));
		assertNotNull(commune);
		assertEquals(5434, commune.getNoOFS());
		assertEquals("Saint-George", commune.getNomOfficiel());
	}

	/**
	 * [SIFISC-9707] Effet de charge ponctuelle qui fait que quelqu'un associe de mauvaises données à un numéro OFS donné ?
	 */
	@Test
	public void testChargeForceeAvecCacheUnireg() throws Exception {
		final int NB_THREADS = 50;
		final int NB_CALLS = 1000;
		final int[] OFS = {8212, 8215, 8100};
		final Random rnd = new Random();
		final InfrastructureConnector serviceAvecCache = getBean(InfrastructureConnector.class, "infrastructureConnectorCache");

		final class GetPays implements Runnable {
			private final int noOfs;

			private GetPays(int noOfs) {
				this.noOfs = noOfs;
			}

			@Override
			public void run() {
				final Pays pays = serviceAvecCache.getPays(noOfs, RegDate.get().addDays(-rnd.nextInt(3650)));      // date au hasard dans les 10 dernières années
				if (pays != null && pays.getNoOFS() != noOfs) {
					throw new RuntimeException("Mauvais pays !!! Demandé " + noOfs + " et reçu " + pays.getNoOFS());
				}
			}
		}

		final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
		final List<Future<?>> futures = new ArrayList<>(NB_CALLS);
		for (int i = 0 ; i < NB_CALLS ; ++ i) {
			futures.add(executor.submit(new GetPays(OFS[i % OFS.length])));
		}
		executor.shutdown();
		for (Future<?> future : futures) {
			future.get();
		}
	}

	/**
	 * [SIFISC-9707] Effet de charge ponctuelle qui fait que quelqu'un associe de mauvaises données à un numéro OFS donné ?
	 */
	@Test
	public void testChargeForceeSansCacheUnireg() throws Exception {
		final int NB_THREADS = 50;
		final int NB_CALLS = 1000;
		final int[] OFS = {8212, 8215, 8100};
		final Random rnd = new Random();
		final InfrastructureConnector serviceSansCache = getBean(InfrastructureConnector.class, "infrastructureConnectorMarshaller");

		final class GetPays implements Runnable {
			private final int noOfs;

			private GetPays(int noOfs) {
				this.noOfs = noOfs;
			}

			@Override
			public void run() {
				final Pays pays = serviceSansCache .getPays(noOfs, RegDate.get().addDays(-rnd.nextInt(3650)));      // date au hasard dans les 10 dernières années
				if (pays != null && pays.getNoOFS() != noOfs) {
					throw new RuntimeException("Mauvais pays !!! Demandé " + noOfs + " et reçu " + pays.getNoOFS());
				}
			}
		}

		final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
		final List<Future<?>> futures = new ArrayList<>(NB_CALLS);
		for (int i = 0 ; i < NB_CALLS ; ++ i) {
			futures.add(executor.submit(new GetPays(OFS[i % OFS.length])));
		}
		executor.shutdown();
		for (Future<?> future : futures) {
			future.get();
		}
	}

	/**
	 * [SIFISC-9707] Effet de charge ponctuelle qui fait que quelqu'un associe de mauvaises données à un numéro OFS donné ?
	 */
	@Test
	public void testChargeForceeDirectementClientFiDoR() throws Exception {
		final int NB_THREADS = 50;
		final int NB_CALLS = 1000;
		final int[] OFS = {8212, 8215, 8100};
		final Random rnd = new Random();
		final FidorClient fidorClient = getBean(FidorClient.class, "fidorClient");

		final class GetPays implements Runnable {
			private final int noOfs;

			private GetPays(int noOfs) {
				this.noOfs = noOfs;
			}

			@Override
			public void run() {
				final Country pays = fidorClient.getPaysDetail(noOfs, RegDate.get().addDays(-rnd.nextInt(3650)));      // date au hasard dans les 10 dernières années
				if (pays != null && pays.getCountry().getId() != noOfs) {
					throw new RuntimeException("Mauvais pays !!! Demandé " + noOfs + " et reçu " + pays.getCountry().getId());
				}
			}
		}

		final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
		final List<Future<?>> futures = new ArrayList<>(NB_CALLS);
		for (int i = 0 ; i < NB_CALLS ; ++ i) {
			futures.add(executor.submit(new GetPays(OFS[i % OFS.length])));
		}
		executor.shutdown();
		for (Future<?> future : futures) {
			future.get();
		}
	}
}
