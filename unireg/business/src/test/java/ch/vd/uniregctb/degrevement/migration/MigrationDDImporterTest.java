package ch.vd.uniregctb.degrevement.migration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MigrationDDImporterTest {

	@Test
	public void testParsedixmillePercent() throws Exception {
		assertEquals(10000, MigrationDDImporter.parsePourdixmille("100"));
		assertEquals(9850, MigrationDDImporter.parsePourdixmille("98.5"));
		assertEquals(150, MigrationDDImporter.parsePourdixmille("1.5"));
		assertEquals(1220, MigrationDDImporter.parsePourdixmille("12.2"));
		assertEquals(0, MigrationDDImporter.parsePourdixmille("0"));
	}
}