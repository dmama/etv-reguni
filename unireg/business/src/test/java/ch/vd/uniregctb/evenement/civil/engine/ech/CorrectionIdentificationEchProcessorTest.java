package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class CorrectionIdentificationEchProcessorTest extends AbstractCorrectionEchProcessorTest {

	@Test(timeout = 10000L)
	public void testCorrectionIdentification() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_IDENTIFICATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON);
	}
}
