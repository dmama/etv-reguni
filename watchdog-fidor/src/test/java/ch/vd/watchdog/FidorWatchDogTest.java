package ch.vd.watchdog;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.evd0012.v1.Logiciel;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que les différents déploiements de Fidor dans les différents environnements sont accessibles.
 */
public abstract class FidorWatchDogTest {

	protected FidorClient fidorClient;

	@Before
	public void setUp() throws Exception {
		fidorClient = connectToFidor();
	}

	protected abstract FidorClient connectToFidor();

	@Test
	public void testGetPays() {
		final Country suisse = fidorClient.getPaysDetail(8100, RegDate.get());
		assertNotNull(suisse);
		assertEquals("Suisse", suisse.getCountry().getShortNameFr());
	}

	@Test
	public void testGetLogiciel() {
		final Logiciel logiciel = fidorClient.getLogicielDetail(1);
		assertNotNull(logiciel);
		assertEquals("Epsitec SA", logiciel.getFournisseur());
	}

	@Test
	public void testGetCommuneParNoOFS() throws Exception {
		final CommuneFiscale commune = fidorClient.getCommuneParNoOFS(5586, RegDate.get(2000, 1, 1));
		assertNotNull(commune);
		assertEquals("Lausanne", commune.getNomOfficiel());
	}

	@Test
	public void testGetCommunesParNoOFS() throws Exception {
		final List<CommuneFiscale> communes = fidorClient.getCommunesParNoOFS(5586);
		assertEquals(1, communes.size());

		final CommuneFiscale commune = communes.get(0);
		assertNotNull(commune);
		assertEquals("Lausanne", commune.getNomOfficiel());
	}

	@Test
	public void testGetCommuneParBatiment() throws Exception {
		final CommuneFiscale commune = fidorClient.getCommuneParBatiment(280011227, RegDate.get(2000, 1, 1));
		assertNotNull(commune);
		assertEquals("Riex", commune.getNomOfficiel());
	}

	@Test
	public void testGetToutesLesCommunes() throws Exception {
		final List<CommuneFiscale> communes = fidorClient.getToutesLesCommunes();
		assertNotNull(communes);
		assertTrue(communes.size() > 3000);
	}
}
