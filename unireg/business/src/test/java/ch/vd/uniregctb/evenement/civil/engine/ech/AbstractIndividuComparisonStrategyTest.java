package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Assert;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.BusinessTest;

public abstract class AbstractIndividuComparisonStrategyTest extends BusinessTest {

	protected void assertNeutre(IndividuComparisonStrategy strategy, long noEvtOriginel, long noEvtCorrection) {
		final Mutable<String> dh = new MutableObject<>();
		final boolean neutre = ask(strategy, noEvtOriginel, noEvtCorrection, dh);
		Assert.assertTrue(neutre);
		Assert.assertNull(dh.getValue());
	}

	protected void assertNonNeutre(IndividuComparisonStrategy strategy, long noEvtOriginel, long noEvtCorrection, String expectedAttribute) {
		final Mutable<String> dh = new MutableObject<>();
		final boolean neutre = ask(strategy, noEvtOriginel, noEvtCorrection, dh);
		Assert.assertFalse(neutre);
		Assert.assertEquals(expectedAttribute, dh.getValue());
	}

	private boolean ask(IndividuComparisonStrategy strategy, long noEvtOriginel, long noEvtCorrection, Mutable<String> dh) {
		final IndividuApresEvenement iae1 = serviceCivil.getIndividuAfterEvent(noEvtOriginel);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuAfterEvent(noEvtCorrection);
		Assert.assertNotNull(iae1);

		return strategy.isFiscalementNeutre(iae1, iae2, dh);
	}
}
