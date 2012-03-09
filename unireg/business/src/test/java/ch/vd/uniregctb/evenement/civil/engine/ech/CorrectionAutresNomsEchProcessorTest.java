package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class CorrectionAutresNomsEchProcessorTest extends AbstractCorrectionEchProcessorTest {

	@Test(timeout = 10000L)
	public void testCorrectionAutresNoms() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.PREMIERE_LIVRAISON);
	}

	@Test(timeout = 10000L)
	public void testCorrectionCorrectionAutresNoms() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.CORRECTION);
	}

	@Test(timeout = 10000L)
	public void testAnnulationCorrectionAutresNoms() throws Exception {
		doTest(TypeEvenementCivilEch.CORR_AUTRES_NOMS, ActionEvenementCivilEch.ANNULATION);
	}
}
