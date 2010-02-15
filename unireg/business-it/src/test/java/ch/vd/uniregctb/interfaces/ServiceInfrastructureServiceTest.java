package ch.vd.uniregctb.interfaces;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

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

		Canton vaud = service.getVaud();
		assertNotNull(vaud);
		assertEquals(22, vaud.getNoOFS());

		Commune lausanne = service.getCommuneByNumeroOfsEtendu(5586);
		assertNotNull(lausanne);
		assertEquals("Lausanne", lausanne.getNomMinuscule());

		assertTrue(service.estDansLeCanton(lausanne));
	}

	@Test
	public void testGetPaysByCode() throws Exception {

		Pays ch = service.getPays("CH");
		assertEquals("CH", ch.getSigleOFS());

		Pays be = service.getPays("BE");
		assertEquals("BE", be.getSigleOFS());
	}
}
