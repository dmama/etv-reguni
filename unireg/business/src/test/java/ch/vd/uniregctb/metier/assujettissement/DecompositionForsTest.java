package ch.vd.uniregctb.metier.assujettissement;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class DecompositionForsTest extends MetierTest {

	/**
	 * Cas du contribuable n°16109718.
	 */
	@SuppressWarnings({"deprecation"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDecompositionFermetureForPrincipalFinAnnee() throws Exception {

		final RegDate dateAchat = date(1998, 10, 17);
		final RegDate dateArrivee = date(2003, 1, 1);

		final Contribuable ctb = createContribuableSansFor();
		final ForFiscalPrincipal ffp2002 = addForPrincipal(ctb, dateAchat, MotifFor.INDETERMINE, dateArrivee.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockPays.PaysInconnu);
		final ForFiscalPrincipal ffp2003 = addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Leysin);
		final ForFiscalSecondaire ffs = addForSecondaire(ctb, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// 2002
		{
			final DecompositionFors decomp = new DecompositionForsAnneeComplete(ctb, 2002);
			assertEquals(date(2002, 1, 1), decomp.debut);
			assertEquals(date(2002, 12, 31), decomp.fin);
			assertFalse(decomp.isEmpty());
			assertFalse(decomp.isFullyEmpty());

			// Vérification des fors principaux

			assertSame(ffp2002, decomp.principalAvantLaPeriode);
			assertSame(ffp2002, decomp.principal);
			assertSame(ffp2003, decomp.principalApresLaPeriode);

			assertNotNull(decomp.principauxDansLaPeriode);
			assertEquals(1, decomp.principauxDansLaPeriode.size());
			assertSame(ffp2002, decomp.principauxDansLaPeriode.get(0));

			// Vérification des fors secondaires

			assertNotNull(decomp.secondaires);
			assertEquals(1, decomp.secondaires.size());
			assertSame(ffs, decomp.secondaires.get(0));

			assertNotNull(decomp.secondairesAvantLaPeriode);
			assertEquals(1, decomp.secondairesAvantLaPeriode.size());
			assertSame(ffs, decomp.secondairesAvantLaPeriode.get(0));

			assertNotNull(decomp.secondairesDansLaPeriode);
			assertEquals(1, decomp.secondairesDansLaPeriode.size());
			assertSame(ffs, decomp.secondairesDansLaPeriode.get(0));

			assertNotNull(decomp.secondairesApresLaPeriode);
			assertEquals(1, decomp.secondairesApresLaPeriode.size());
			assertSame(ffs, decomp.secondairesApresLaPeriode.get(0));
		}

		// 2003
		{
			final DecompositionFors decomp = new DecompositionForsAnneeComplete(ctb, 2003);
			assertEquals(date(2003, 1, 1), decomp.debut);
			assertEquals(date(2003, 12, 31), decomp.fin);
			assertFalse(decomp.isEmpty());
			assertFalse(decomp.isFullyEmpty());

			// Vérification des fors principaux

			assertSame(ffp2002, decomp.principalAvantLaPeriode);
			assertSame(ffp2003, decomp.principal);
			assertSame(ffp2003, decomp.principalApresLaPeriode);

			assertNotNull(decomp.principauxDansLaPeriode);
			assertEquals(1, decomp.principauxDansLaPeriode.size());
			assertSame(ffp2003, decomp.principauxDansLaPeriode.get(0));

			// Vérification des fors secondaires

			assertNotNull(decomp.secondaires);
			assertEquals(1, decomp.secondaires.size());
			assertSame(ffs, decomp.secondaires.get(0));

			assertNotNull(decomp.secondairesAvantLaPeriode);
			assertEquals(1, decomp.secondairesAvantLaPeriode.size());
			assertSame(ffs, decomp.secondairesAvantLaPeriode.get(0));

			assertNotNull(decomp.secondairesDansLaPeriode);
			assertEquals(1, decomp.secondairesDansLaPeriode.size());
			assertSame(ffs, decomp.secondairesDansLaPeriode.get(0));

			assertNotNull(decomp.secondairesApresLaPeriode);
			assertEquals(1, decomp.secondairesApresLaPeriode.size());
			assertSame(ffs, decomp.secondairesApresLaPeriode.get(0));
		}
	}
}
