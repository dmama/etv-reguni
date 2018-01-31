package ch.vd.uniregctb.registrefoncier;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertTrue;

public class RaisonAcquisitionRFTest {

	@Test
	public void testCompareTo() throws Exception {

		final RaisonAcquisitionRF r1 = new RaisonAcquisitionRF(null, null, null);
		final RaisonAcquisitionRF r2 = new RaisonAcquisitionRF(RegDate.get(1990, 1, 1), null, null);
		final RaisonAcquisitionRF r3 = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), null, null);

		// dates nulles en premier
		assertTrue(r1.compareTo(r2) < 0);
		assertTrue(r1.compareTo(r3) < 0);

		// tri sur les dates
		assertTrue(r2.compareTo(r1) > 0);
		assertTrue(r2.compareTo(r3) < 0);

		// tri sur les dates (bis)
		assertTrue(r3.compareTo(r1) > 0);
		assertTrue(r3.compareTo(r2) > 0);
	}

	@Test
	public void testCompareToDatesIdentiques() throws Exception {

		final RaisonAcquisitionRF r1 = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), null, null);
		final RaisonAcquisitionRF r2 = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), null, new IdentifiantAffaireRF(6, 2006, 1402, 0));
		final RaisonAcquisitionRF r3 = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), null, new IdentifiantAffaireRF(6, 2006, 1410, 0));

		// numéros d'affaires nuls en premier
		assertTrue(r1.compareTo(r2) < 0);
		assertTrue(r1.compareTo(r3) < 0);

		// tri sur les numéros d'affaire
		assertTrue(r2.compareTo(r1) > 0);
		assertTrue(r2.compareTo(r3) < 0);

		// tri sur les numéros d'affaire (bis)
		assertTrue(r3.compareTo(r1) > 0);
		assertTrue(r3.compareTo(r2) > 0);
	}

	@Test
	public void testCompareToDatesEtNumerosAffaireIdentiques() throws Exception {

		final RaisonAcquisitionRF r1 = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), null, new IdentifiantAffaireRF(6, 2006, 1402, 0));
		final RaisonAcquisitionRF r2 = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Achat", new IdentifiantAffaireRF(6, 2006, 1402, 0));
		final RaisonAcquisitionRF r3 = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2006, 1402, 0));

		// motifs d'acquisition nuls en premier
		assertTrue(r1.compareTo(r2) < 0);
		assertTrue(r1.compareTo(r3) < 0);

		// tri sur les motifs d'acquisition
		assertTrue(r2.compareTo(r1) > 0);
		assertTrue(r2.compareTo(r3) < 0);

		// tri sur les motifs d'acquisition (bis)
		assertTrue(r3.compareTo(r1) > 0);
		assertTrue(r3.compareTo(r2) > 0);
	}
}