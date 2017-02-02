package ch.vd.uniregctb.foncier.migration.ici;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class MigrationDDImporterTest extends WithoutSpringTest {

	/**
	 * Ce test vérifie que la demande de dégrèvement la plus récente est bien retournée.
	 */
	@Test
	public void testGetDDToSaveOneDD() throws Exception {

		final MigrationDDImporterResults rapport = new MigrationDDImporterResults(1);
		final MigrationDD dd0 = new MigrationDD();

		assertSame(dd0, MigrationDDImporter.getDDToSave(Collections.singletonList(dd0), rapport));
		assertEmpty(rapport.getDemandesIgnorees());
	}

	/**
	 * Ce test vérifie que la demande de dégrèvement la plus récente est bien retournée.
	 */
	@Test
	public void testGetDDToSaveTwoDDs() throws Exception {

		final MigrationDDImporterResults rapport = new MigrationDDImporterResults(1);

		// deux demandes de migration
		final MigrationDD dd0 = new MigrationDD();
		dd0.setAnneeFiscale(2013);
		final MigrationDD dd1 = new MigrationDD();
		dd1.setAnneeFiscale(2015);

		assertSame(dd1, MigrationDDImporter.getDDToSave(Arrays.asList(dd0, dd1), rapport));
		final List<MigrationDDImporterResults.DemandeInfo> ignored = rapport.getDemandesIgnorees();
		assertEquals(1, ignored.size());
		assertEquals("Une demande de dégrèvement plus récente (2015) existe dans l'export (cette demande = 2013).", ignored.get(0).getMessage());
	}

}