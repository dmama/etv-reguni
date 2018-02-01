package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class CorrectionIdentificationEchProcessorTest extends AbstractCorrectionEchProcessorTest {

	@Test(timeout = 10000L)
	public void testAnnonceCorrectionIdentification() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON);
	}

	@Test(timeout = 10000L)
	public void testCorrectionCorrectionIdentification() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.CORRECTION);
	}

	@Test(timeout = 10000L)
	public void testAnnulationCorrectionIdentification() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.ANNULATION);
	}
}
