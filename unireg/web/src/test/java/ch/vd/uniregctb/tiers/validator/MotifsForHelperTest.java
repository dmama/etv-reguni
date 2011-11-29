package ch.vd.uniregctb.tiers.validator;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

public class MotifsForHelperTest extends WithoutSpringTest {

	@Test
	public void testMotifsOuvertureSurTousMotifsRattachement() throws Exception {
		for (MotifRattachement motifRattachement : MotifRattachement.values()) {
			final MotifsForHelper.TypeFor type = new MotifsForHelper.TypeFor(NatureTiers.Habitant, GenreImpot.REVENU_FORTUNE, motifRattachement);
			final List<MotifFor> motifsOuverture = MotifsForHelper.getMotifsOuverture(type);
			Assert.assertNotNull("Rattachement " + motifRattachement, motifsOuverture);
			Assert.assertTrue("Rattachement " + motifRattachement, !motifsOuverture.isEmpty());
		}
	}

	@Test
	public void testMotifsFermetureSurTousMotifsRattachement() throws Exception {
		for (MotifRattachement motifRattachement : MotifRattachement.values()) {
			final MotifsForHelper.TypeFor type = new MotifsForHelper.TypeFor(NatureTiers.Habitant, GenreImpot.REVENU_FORTUNE, motifRattachement);
			final List<MotifFor> motifsFermeture = MotifsForHelper.getMotifsFermeture(type);
			Assert.assertNotNull("Rattachement " + motifRattachement, motifsFermeture);
			Assert.assertTrue("Rattachement " + motifRattachement, !motifsFermeture.isEmpty());
		}
	}
}
