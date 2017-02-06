package ch.vd.uniregctb.foncier.migration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MigrationParcelleTest {

	/**
	 * Ce test vérifie que le numéro de base de la parcelle est bien utilisé si le numéro de parcelle manque. Version "Tiret"
	 */
	@Test
	public void testConstructeurBaseParcelleTirets() throws Exception {
		assertParcelle(123, null, null, null, new MigrationParcelle("123", null, null));
		assertParcelle(123, 34, null, null, new MigrationParcelle("123-34", null, null));
		assertParcelle(123, null, null, null, new MigrationParcelle("123", null, "34"));
		assertParcelle(123, 34, 4, null, new MigrationParcelle("123-34-4", null, null));
		assertParcelle(123, 34, 4, 1, new MigrationParcelle("123-34-4-1", null, null));
	}

	/**
	 * Ce test vérifie que le numéro de base de la parcelle est bien utilisé si le numéro de parcelle manque. Version "Slash"
	 */
	@Test
	public void testConstructeurBaseParcelleSlashes() throws Exception {
		assertParcelle(123, null, null, null, new MigrationParcelle("123", null, null));
		assertParcelle(123, 34, null, null, new MigrationParcelle("123/34", null, null));
		assertParcelle(123, null, null, null, new MigrationParcelle("123", null, "34"));
		assertParcelle(123, 34, 4, null, new MigrationParcelle("123/34/4", null, null));
		assertParcelle(123, 34, 4, 1, new MigrationParcelle("123/34/4/1", null, null));
	}

	/**
	 * Ce test vérifie que le numéro de base de la parcelle est ignoré si le numéro de parcelle est spécifié. Version "Tiret"
	 */
	@Test
	public void testConstructeurBaseNoParcelleEtNoParcelleTirets() throws Exception {
		assertParcelle(123, null, null, null, new MigrationParcelle("100", "123", null));
		assertParcelle(123, 34, null, null, new MigrationParcelle("100", "123-34", null));
		assertParcelle(123, null, null, null, new MigrationParcelle("100", "123", "34"));
		assertParcelle(123, 34, 4, null, new MigrationParcelle("100", "123-34-4", null));
		assertParcelle(123, 34, 4, 1, new MigrationParcelle("100", "123-34-4-1", null));
	}

	/**
	 * Ce test vérifie que le numéro de base de la parcelle est ignoré si le numéro de parcelle est spécifié. Version "Slash"
	 */
	@Test
	public void testConstructeurBaseNoParcelleEtNoParcelleSlashes() throws Exception {
		assertParcelle(123, null, null, null, new MigrationParcelle("100", "123", null));
		assertParcelle(123, 34, null, null, new MigrationParcelle("100", "123/34", null));
		assertParcelle(123, null, null, null, new MigrationParcelle("100", "123", "34"));
		assertParcelle(123, 34, 4, null, new MigrationParcelle("100", "123/34/4", null));
		assertParcelle(123, 34, 4, 1, new MigrationParcelle("100", "123/34/4/1", null));
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("123/null/null/null", new MigrationParcelle("123", null, null).toString());
		assertEquals("123/34/null/null", new MigrationParcelle("123-34", null, null).toString());
		assertEquals("123/34/4/null", new MigrationParcelle("123-34-4", null, null).toString());
		assertEquals("123/34/4/1", new MigrationParcelle("123-34-4-1", null, null).toString());
	}

	@Test
	public void testCFA() throws Exception {
		assertParcelle(3019, null, null, null, new MigrationParcelle("3019-CFA", null, null));
		assertParcelle(3019, null, null, null, new MigrationParcelle("4545", "3019-CFA", null));
		assertParcelle(971, null, null, null, new MigrationParcelle("971-CFA2", null, null));
		assertParcelle(971, null, null, null, new MigrationParcelle("8451", "971-CFA2", null));
	}

	@Test
	public void testNumeroParcelleRessembleADate() {
		assertParcelle(4871, 4, null, null, new MigrationParcelle("4871-4", "11.22.3333", null));
		assertParcelle(4871, 3, null, null, new MigrationParcelle("4871-3", "11.mai", null));
		assertParcelle(48743, 2, null, null, new MigrationParcelle("48743-2", "févr.15", null));
	}

	private void assertParcelle(int noParcelle, Integer index1, Integer index2, Integer index3, MigrationParcelle parcelle) {
		assertEquals(noParcelle, parcelle.getNoParcelle());
		assertEquals(index1, parcelle.getIndex1());
		assertEquals(index2, parcelle.getIndex2());
		assertEquals(index3, parcelle.getIndex3());
	}
}