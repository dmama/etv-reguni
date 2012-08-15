package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.DataHolder;

public abstract class AbstractIndividuComparisonStrategyTest extends BusinessTest {

	protected void assertNeutre(IndividuComparisonStrategy strategy, long noEvtOriginel, long noEvtCorrection) {
		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = ask(strategy, noEvtOriginel, noEvtCorrection, dh);
		Assert.assertTrue(neutre);
		Assert.assertNull(dh.get());
	}

	protected void assertNonNeutre(IndividuComparisonStrategy strategy, long noEvtOriginel, long noEvtCorrection, String expectedAttribute) {
		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = ask(strategy, noEvtOriginel, noEvtCorrection, dh);
		Assert.assertFalse(neutre);
		Assert.assertEquals(expectedAttribute, dh.get());
	}

	private boolean ask(IndividuComparisonStrategy strategy, long noEvtOriginel, long noEvtCorrection, DataHolder<String> dh) {
		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvtOriginel);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvtCorrection);
		Assert.assertNotNull(iae1);

		return strategy.isFiscalementNeutre(iae1, iae2, dh);
	}
}
