package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class CorrectionRelationAnnonceEchProcessorTest extends AbstractCorrectionEchProcessorTest {

	@Test(timeout = 10000L)
	public void testCorrectionRelationAnnonce() throws Exception {
		test();
	}

	@Override
	protected TypeEvenementCivilEch getTypeEvenementCorrection() {
		return TypeEvenementCivilEch.CORR_RELATION_ANNONCE;
	}
}
