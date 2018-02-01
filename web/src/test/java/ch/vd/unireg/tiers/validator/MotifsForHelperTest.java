package ch.vd.uniregctb.tiers.validator;

import java.util.EnumSet;
import java.util.Set;

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
			final Set<MotifFor> motifsOuverture = MotifsForHelper.getMotifsOuverture(type);
			Assert.assertNotNull("Rattachement " + motifRattachement, motifsOuverture);
			Assert.assertTrue("Rattachement " + motifRattachement, !motifsOuverture.isEmpty());
		}
	}

	@Test
	public void testMotifsFermetureSurTousMotifsRattachement() throws Exception {
		for (MotifRattachement motifRattachement : MotifRattachement.values()) {
			final MotifsForHelper.TypeFor type = new MotifsForHelper.TypeFor(NatureTiers.Habitant, GenreImpot.REVENU_FORTUNE, motifRattachement);
			final Set<MotifFor> motifsFermeture = MotifsForHelper.getMotifsFermeture(type);
			Assert.assertNotNull("Rattachement " + motifRattachement, motifsFermeture);
			Assert.assertTrue("Rattachement " + motifRattachement, !motifsFermeture.isEmpty());
		}
	}

	/**
	 * [SIFISC-11145] Suppression de la possibilité de fermer les fors directement dans l'IHM avec certains motifs (déménagement, départ HC/HS, arrivée HC/HS, permis C/Suisse)
	 */
	@Test
	public void testMotifsFermetureDomicile() throws Exception {
		{
			final MotifsForHelper.TypeFor type = new MotifsForHelper.TypeFor(NatureTiers.Habitant, GenreImpot.REVENU_FORTUNE, MotifRattachement.DOMICILE);
			final Set<MotifFor> motifsFermeture = MotifsForHelper.getMotifsFermeture(type);
			Assert.assertEquals(EnumSet.of(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.FUSION_COMMUNES), motifsFermeture);
		}
		{
			final MotifsForHelper.TypeFor type = new MotifsForHelper.TypeFor(NatureTiers.NonHabitant, GenreImpot.REVENU_FORTUNE, MotifRattachement.DOMICILE);
			final Set<MotifFor> motifsFermeture = MotifsForHelper.getMotifsFermeture(type);
			Assert.assertEquals(EnumSet.of(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.FUSION_COMMUNES), motifsFermeture);
		}
		{
			final MotifsForHelper.TypeFor type = new MotifsForHelper.TypeFor(NatureTiers.MenageCommun, GenreImpot.REVENU_FORTUNE, MotifRattachement.DOMICILE);
			final Set<MotifFor> motifsFermeture = MotifsForHelper.getMotifsFermeture(type);
			Assert.assertEquals(EnumSet.of(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.FUSION_COMMUNES, MotifFor.VEUVAGE_DECES), motifsFermeture);
		}
	}
}
