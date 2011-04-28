package ch.vd.uniregctb.interfaces;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Jean Eric CUENDET
 *
 */
public class ServiceInfrastructureServiceTest extends BusinessItTest {

	private static final Logger LOGGER = Logger.getLogger(ServiceInfrastructureServiceTest.class);

	/**
	 *
	 */
	private ServiceInfrastructureService service;

	public ServiceInfrastructureServiceTest() {
	}

	/**
	 * @throws Exception
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testSuisse() throws Exception {
		assertEquals("CH", service.getSuisse().getSigleOFS());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCantons() throws Exception {
		assertEquals(27, service.getAllCantons().size());
	}

	/**
	 * @throws Exception
	 */
	public void testCommunesDeVaud() throws Exception {
		assertEquals(396, service.getCommunesDeVaud().size());
	}

	/**
	 * @throws Exception
	 */
	public void testCantonDeZurich() throws Exception {
		Canton canton = service.getCantonBySigle("ZH");
		assertNotNull(canton);
		assertEquals("ZH", canton.getSigleOFS());
	}

	/**
	 * @throws Exception
	 */
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

	/**
	 * @throws Exception
	 */
	public void testCommunes() throws Exception {
		Canton cantonZH = service.getCantonBySigle("ZH");
		assertEquals(171, service.getListeCommunes(cantonZH.getNoOFS()).size());

		Canton cantonGE = service.getCantonBySigle("GE");
		assertEquals(45, service.getListeCommunes(cantonGE.getNoOFS()).size());
	}

	/**
	 * @throws Exception
	 */
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

		final Commune lausanne = service.getCommuneByNumeroOfsEtendu(5586, null);
		assertNotNull(lausanne);
		assertEquals("Lausanne", lausanne.getNomMinuscule());

		assertTrue(service.estDansLeCanton(lausanne));
	}

	@Test
	public void testGetPaysByCode() throws Exception {

		final Pays ch = service.getPays("CH");
		assertEquals("CH", ch.getSigleOFS());
		assertEquals("Suisse", ch.getNomMinuscule());

		final Pays fr = service.getPays("FR");
		assertEquals("FR", fr.getSigleOFS());
		assertEquals("France", fr.getNomMinuscule());

		final Pays be = service.getPays("BE");
		assertEquals("BE", be.getSigleOFS());
		assertEquals("Belgique", be.getNomMinuscule());
	}

	@Test
	public void testDateValiditeCommuneLusseryVillars() throws Exception {

		final int noOfsLussery = 5487;
		{
			final Commune lussery = service.getCommuneByNumeroOfsEtendu(noOfsLussery, RegDate.get(1998, 12, 1));    // fusion au 31.12.1998
			assertNotNull(lussery);
			// FIXME (msi) en attente de la résolution de SIFISC-761
			// assertEquals("Lussery", lussery.getNomMinuscule());
		}
		{
			final Commune lusseryVillars = service.getCommuneByNumeroOfsEtendu(noOfsLussery, RegDate.get(1999, 1, 1));  // fusion au 31.12.1998
			assertNotNull(lusseryVillars);
			assertEquals("Lussery-Villars", lusseryVillars.getNomMinuscule());
		}
		{
			final Commune lusseryVillars = service.getCommuneByNumeroOfsEtendu(noOfsLussery, null);          // commune toujours ouverte
			assertNotNull(lusseryVillars);
			assertEquals("Lussery-Villars", lusseryVillars.getNomMinuscule());
		}
	}

	@Test
	public void testDateValiditeCommuneCloturee() throws Exception {

		final int noOfsHerlisberg = 1029;       // commune clôturée le 31.12.2004
		{
			final Commune herlisberg = service.getCommuneByNumeroOfsEtendu(noOfsHerlisberg, RegDate.get(2000, 1, 1));
			assertNotNull(herlisberg);      // la date ne devrait pas être prise en compte puisqu'il n'y a qu'une seule commune
			assertEquals(RegDate.get(2004, 12, 31), herlisberg.getDateFinValidite());
		}
		{
			final Commune herlisberg = service.getCommuneByNumeroOfsEtendu(noOfsHerlisberg, RegDate.get(2005, 1, 1));
			assertNotNull(herlisberg);      // la date ne devrait pas être prise en compte puisqu'il n'y a qu'une seule commune
			assertEquals(RegDate.get(2004, 12, 31), herlisberg.getDateFinValidite());
		}
		{
			final Commune herlisberg = service.getCommuneByNumeroOfsEtendu(noOfsHerlisberg, null);
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
			assertEquals(MockCommune.Riex.getNoOFSEtendu(), commune.getNoOFSEtendu());
			assertEquals("Riex", commune.getNomMinuscule());
		}

		// après fusion civile MAIS avant fusion fiscale
		{
			final Commune commune = service.getCommuneByEgid(immeuble, date(2011, 10, 1));
			assertNotNull(commune);
			assertEquals(MockCommune.Riex.getNoOFSEtendu(), commune.getNoOFSEtendu());
			assertEquals("Riex", commune.getNomMinuscule());
		}

		// après fusion civile ET après fusion fiscale
		{
			final Commune commune = service.getCommuneByEgid(immeuble, date(2012, 1, 1));
			assertNotNull(commune);
			assertEquals(MockCommune.BourgEnLavaux.getNoOFSEtendu(), commune.getNoOFSEtendu());
			// FIXME (msi) en attente de la résolution de SIFISC-628 : assertEquals("Bourg-en-Lavaux", commune.getNomMinuscule());
		}
	}

	// FIXME (msi) en attente de la résolution de SIFISC-766
	@Ignore
	@Test
	public void testGetCommuneHistoByNumeroOFS() throws Exception {

		final List<Commune> list = service.getCommuneHistoByNumeroOfs(MockCommune.BourgEnLavaux.getNoOFSEtendu());
		assertNotNull(list);
		assertEquals(1, list.size());

		final Commune commune = list.get(0);
		assertEquals(MockCommune.BourgEnLavaux.getNoOFSEtendu(), commune.getNoOFSEtendu());
		assertEquals("Bourg-en-Lavaux", commune.getNomMinuscule());
		assertEquals(RegDate.get(2012, 1, 1), commune.getDateDebutValidite());
	}
}
