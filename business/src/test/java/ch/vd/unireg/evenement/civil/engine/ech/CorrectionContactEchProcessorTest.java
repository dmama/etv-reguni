package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class CorrectionContactEchProcessorTest extends AbstractCorrectionEchProcessorTest {

	@Test(timeout = 10000L)
	public void testAnnonceCorrectionContact() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.PREMIERE_LIVRAISON);
	}

	@Test(timeout = 10000L)
	public void testAnnulationCorrectionContact() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.ANNULATION);
	}

	@Test(timeout = 10000L)
	public void testCorrectionCorrectionContact() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.CORRECTION);
	}
}
