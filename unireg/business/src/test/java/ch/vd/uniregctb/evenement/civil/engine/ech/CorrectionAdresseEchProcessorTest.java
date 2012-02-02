package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class CorrectionAdresseEchProcessorTest extends AbstractCorrectionEchProcessorTest {

	@Test(timeout = 10000L)
	public void testCorrectionAdresse() throws Exception {
		test();
	}

	@Override
	protected TypeEvenementCivilEch getTypeEvenementCorrection() {
		return TypeEvenementCivilEch.CORR_ADRESSE;
	}
}
