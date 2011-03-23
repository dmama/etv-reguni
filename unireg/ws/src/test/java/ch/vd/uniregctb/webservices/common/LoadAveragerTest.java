package ch.vd.uniregctb.webservices.common;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class LoadAveragerTest extends WithoutSpringTest {

	@Test
	public void testAverage() throws Exception {

		// ne sert pas pendant ce test
		final LoadMonitorable dummy = new LoadMonitorable() {
			public int getLoad() {
				throw new NotImplementedException();
			}
		};

		final int nbPoints = 100;
		final LoadAverager averager = new LoadAverager(dummy, "test", nbPoints, 1);

		// à vide
		Assert.assertEquals(0.0, averager.getAverageLoad(), 1e-12);

		// on le remplit peu à peu
		for (int i = 0 ; i < nbPoints ; ++ i) {
			averager.addSamplingData(i);

			final double expected = (1.0 * i) / 2.0;
			Assert.assertEquals("Après l'ajout de 0 à " + i, expected, averager.getAverageLoad(), 1e-12);
		}

		// puis on dépasse la limite (effet glissant)
		for (int i = nbPoints ; i < 2 * nbPoints ; ++ i) {
			averager.addSamplingData(i);

			final double expected = ((i - nbPoints + 1) + i) / 2.0;
			Assert.assertEquals("Après l'ajout de " + i, expected, averager.getAverageLoad(), 1e-12);
		}

		// si on ajoute ensuite nbPoints fois une valeur fixe, la moyenne doit être cette valeur fixe (effet glissant)
		for (int i = 0 ; i < nbPoints ; ++ i) {
			averager.addSamplingData(20);
		}
		Assert.assertEquals(20, averager.getAverageLoad(), 1e-12);
	}
}
