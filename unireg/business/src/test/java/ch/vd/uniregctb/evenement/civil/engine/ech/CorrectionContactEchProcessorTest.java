package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class CorrectionContactEchProcessorTest extends AbstractCorrectionEchProcessorTest {

	@Test(timeout = 10000L)
	public void testCorrectionContact() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_CONTACT, ActionEvenementCivilEch.PREMIERE_LIVRAISON);
	}
}
