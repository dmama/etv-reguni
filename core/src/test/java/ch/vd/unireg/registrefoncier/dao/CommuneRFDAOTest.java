package ch.vd.unireg.registrefoncier.dao;

import org.junit.Test;

import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.key.CommuneRFKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CommuneRFDAOTest extends CoreDAOTest {

	private CommuneRFDAO communeRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
	}

	/**
	 * [SIFISC-30558] Vérifie que la méthode 'findActive' retrouve bien les communes par numéro RF.
	 */
	@Test
	public void testFindActiveByNoRf() {

		doInNewTransaction(status -> {
			addCommuneRF("Ouchy", 1, 5555);
			return null;
		});

		doInNewTransaction(status -> {
			final CommuneRF commune = communeRFDAO.findActive(new CommuneRFKey(1));
			assertNotNull(commune);
			assertEquals(1, commune.getNoRf());
			assertEquals(5555, commune.getNoOfs());
			assertEquals("Ouchy", commune.getNomRf());
			return null;
		});
	}

	/**
	 * [SIFISC-30558] Vérifie que la méthode 'findActive' retrouve bien les communes par numéro Ofs.
	 */
	@Test
	public void testFindActiveByNoOfs() {

		doInNewTransaction(status -> {
			addCommuneRF("Ouchy", 1, 5555);
			return null;
		});

		doInNewTransaction(status -> {
			final CommuneRF commune = communeRFDAO.findActive(new CommuneRFKey(5555));
			assertNotNull(commune);
			assertEquals(1, commune.getNoRf());
			assertEquals(5555, commune.getNoOfs());
			assertEquals("Ouchy", commune.getNomRf());
			return null;
		});
	}
}