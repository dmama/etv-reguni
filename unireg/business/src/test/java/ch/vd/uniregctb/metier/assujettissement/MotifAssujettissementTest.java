package ch.vd.uniregctb.metier.assujettissement;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.MotifFor;

public class MotifAssujettissementTest extends WithoutSpringTest {

	/**
	 * On vérifie ici que tous les motifs de for ont un pendant chez les motifs d'assujettissement
	 */
	@Test
	public void testMappingPourTousMotifsFors() {
		Assert.assertTrue(MotifAssujettissement.values().length + " vs. " + MotifFor.values().length, MotifAssujettissement.values().length >= MotifFor.values().length);
		Assert.assertNull(MotifAssujettissement.of(null));
		for (MotifFor motifFor : MotifFor.values()) {
			final MotifAssujettissement motifAssujettissement = MotifAssujettissement.of(motifFor);
			Assert.assertNotNull("Motif for " + motifFor + " sans mapping", motifAssujettissement);
		}
	}
}
