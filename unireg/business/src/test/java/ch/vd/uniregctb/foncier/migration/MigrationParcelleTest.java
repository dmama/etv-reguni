package ch.vd.uniregctb.foncier.migration;

import org.junit.Assert;
import org.junit.Test;

public class MigrationParcelleTest {

	/**
	 * Ce test vérifie que le numéro de base de la parcelle est bien utilisé si le numéro de parcelle manque.
	 */
	@Test
	public void testConstructeurBaseParcelle() throws Exception {
		assertParcelle(123, null, null, null, new MigrationParcelle("123", null, null));
		assertParcelle(123, 34, null, null, new MigrationParcelle("123-34", null, null));
		assertParcelle(123, null, null, null, new MigrationParcelle("123", null, "34"));
		assertParcelle(123, 34, 4, null, new MigrationParcelle("123-34-4", null, null));
		assertParcelle(123, 34, 4, 1, new MigrationParcelle("123-34-4-1", null, null));
	}

	/**
	 * Ce test vérifie que le numéro de base de la parcelle est ignoré si le numéro de parcelle est spécifié.
	 */
	@Test
	public void testConstructeurBaseNoParcelleEtNoParcelle() throws Exception {
		assertParcelle(123, null, null, null, new MigrationParcelle("100", "123", null));
		assertParcelle(123, 34, null, null, new MigrationParcelle("100", "123-34", null));
		assertParcelle(123, null, null, null, new MigrationParcelle("100", "123", "34"));
		assertParcelle(123, 34, 4, null, new MigrationParcelle("100", "123-34-4", null));
		assertParcelle(123, 34, 4, 1, new MigrationParcelle("100", "123-34-4-1", null));
	}

	@Test
	public void testToString() throws Exception {
		Assert.assertEquals("123/null/null/null", new MigrationParcelle("123", null, null).toString());
		Assert.assertEquals("123/34/null/null", new MigrationParcelle("123-34", null, null).toString());
		Assert.assertEquals("123/34/4/null", new MigrationParcelle("123-34-4", null, null).toString());
		Assert.assertEquals("123/34/4/1", new MigrationParcelle("123-34-4-1", null, null).toString());
	}

	@Test
	public void testCFA() throws Exception {
		assertParcelle(3019, null, null, null, new MigrationParcelle("3019-CFA", null, null));
		assertParcelle(3019, null, null, null, new MigrationParcelle("4545", "3019-CFA", null));
		assertParcelle(971, null, null, null, new MigrationParcelle("971-CFA2", null, null));
		assertParcelle(971, null, null, null, new MigrationParcelle("8451", "971-CFA2", null));
	}

	@Test
	public void testSlashNonInterprete() throws Exception {
		try {
			new MigrationParcelle("513/1", null, null);
			Assert.fail();
		}
		catch (NumberFormatException e) {
			Assert.assertEquals("For input string: \"513/1\"", e.getMessage());
		}
	}

	@Test
	public void testLettresFinalesIgnorees() throws Exception {
		assertParcelle(4545, null, null, null, new MigrationParcelle("4545A", null, null));
		assertParcelle(3019, null, null, null, new MigrationParcelle("4545", "3019 A", null));
		assertParcelle(8451, null, null, null, new MigrationParcelle("8451 B", null, null));
		assertParcelle(971, null, null, null, new MigrationParcelle("8451", "971B", null));

	}

	private void assertParcelle(int noParcelle, Integer index1, Integer index2, Integer index3, MigrationParcelle parcelle) {
		Assert.assertEquals(noParcelle, parcelle.getNoParcelle());
		Assert.assertEquals(index1, parcelle.getIndex1());
		Assert.assertEquals(index2, parcelle.getIndex2());
		Assert.assertEquals(index3, parcelle.getIndex3());
	}
}