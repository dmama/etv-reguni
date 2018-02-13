package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;

import static org.junit.Assert.assertTrue;

public class RaisonAcquisitionRFHelperTest {

	/**
	 * Ce test s'assure que les raisons annulées sont ignorées par le méthode.
	 */
	@Test
	public void testDataEqualsAvecRaisonsAnnulees() {

		final RaisonAcquisitionRF raison1 = new RaisonAcquisitionRF(RegDate.get(2000,1,1), "Achat", null);
		final RaisonAcquisitionRF raison1annule = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Achat", null);
		raison1annule.setAnnule(true);

		final RaisonAcquisitionRF raison3 = new RaisonAcquisitionRF(RegDate.get(2010,1,1), "Succession", null);
		final RaisonAcquisitionRF raison3copy = new RaisonAcquisitionRF(RegDate.get(2010,1,1), "Succession", null);

		final RaisonAcquisitionRF raison4 = new RaisonAcquisitionRF(RegDate.get(2020,1,1), "Donation", null);
		final RaisonAcquisitionRF raison4copy = new RaisonAcquisitionRF(RegDate.get(2020,1,1), "Donation", null);

		assertTrue(RaisonAcquisitionRFHelper.dataEquals(Arrays.asList(raison1annule), null));
		assertTrue(RaisonAcquisitionRFHelper.dataEquals(null, Arrays.asList(raison1annule)));

		assertTrue(RaisonAcquisitionRFHelper.dataEquals(Arrays.asList(raison1annule), Collections.emptyList()));
		assertTrue(RaisonAcquisitionRFHelper.dataEquals(Collections.emptyList(), Arrays.asList(raison1annule)));

		assertTrue(RaisonAcquisitionRFHelper.dataEquals(Arrays.asList(raison3, raison1annule), Arrays.asList(raison3copy)));
		assertTrue(RaisonAcquisitionRFHelper.dataEquals(Arrays.asList(raison3), Arrays.asList(raison3copy, raison1annule)));

		assertTrue(RaisonAcquisitionRFHelper.dataEquals(Arrays.asList(raison1annule, raison3, raison4), Arrays.asList(raison4copy, raison3copy, raison1annule)));
	}
}