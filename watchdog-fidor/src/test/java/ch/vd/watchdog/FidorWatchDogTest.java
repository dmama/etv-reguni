package ch.vd.watchdog;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorDate;
import ch.vd.fidor.ws.v2.Logiciel;
import ch.vd.fidor.ws.v2.Pays;
import ch.vd.uniregctb.webservice.fidor.FidorClient;

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
		final Pays suisse = fidorClient.getPaysDetail(8100);
		assertNotNull(suisse);
		assertEquals("Suisse", suisse.getNomCourtFr());
	}

	@Test
	public void testGetLogiciel() {
		final Logiciel logiciel = fidorClient.getLogicielDetail(11);
		assertNotNull(logiciel);
		assertEquals("Geste Informatique", logiciel.getFournisseur());
	}

	@Test
	public void testGetCommuneParNoOFS() throws Exception {
		final CommuneFiscale commune = fidorClient.getCommuneParNoOFS(5586, newDate(2000, 1, 1));
		assertNotNull(commune);
		assertEquals("Lausanne", commune.getNomOfficiel());
	}

	@Test
	public void testGetCommuneHistoParNoOFS() throws Exception {
		final List<CommuneFiscale> communes = fidorClient.getCommunesHistoParNoOFS(5586);
		assertEquals(1, communes.size());

		final CommuneFiscale commune = communes.get(0);
		assertNotNull(commune);
		assertEquals("Lausanne", commune.getNomOfficiel());
	}

	@Test
	public void testGetCommuneParBatiment() throws Exception {
		final CommuneFiscale commune = fidorClient.getCommuneParBatiment(280011227, newDate(2000, 1, 1));
		assertNotNull(commune);
		assertEquals("Riex", commune.getNomOfficiel());
	}

	@Test
	public void testGetToutesLesCommunes() throws Exception {
		final List<CommuneFiscale> communes = fidorClient.getToutesLesCommunes();
		assertNotNull(communes);
		assertTrue(communes.size() > 3000);
	}

	private static FidorDate newDate(int year, int month, int day) {
		FidorDate date = new FidorDate();
		date.setYear(year);
		date.setMonth(month);
		date.setDay(day);
		return date;
	}
}
