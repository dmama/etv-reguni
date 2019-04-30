package ch.vd.unireg.fourreNeutre;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.Sexe;

public class ImpressionFourreNeutreHelperImplTest extends BusinessTest {

	final static String CODE_DOCUMENT_FOURRE_NEUTRE = "060140005";

	@Test
	public void testCodeBarrePourPP() throws Exception {
		// mise en place
		final long idTiers = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Francis", "Orange", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Tiers pp = hibernateTemplate.get(Tiers.class, idTiers);
			final FourreNeutre fourre = new FourreNeutre(pp, 2015);

			final String codeBarre = CODE_DOCUMENT_FOURRE_NEUTRE + ImpressionFourreNeutreHelperImpl.calculCodeBarre(fourre);
			final String attendu = "06014000520150" + pp.getNumero() + "0000";
			Assert.assertEquals(attendu, codeBarre);
			return null;
		});
	}

	@Test
	public void testCodeBarrePourPM() throws Exception {
		// mise en place
		final long idTiers = doInNewTransactionAndSession(status -> {
			final Entreprise es = addEntrepriseInconnueAuCivil();
			return es.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Tiers es = hibernateTemplate.get(Tiers.class, idTiers);
			FourreNeutre fourre = new FourreNeutre(es, 2010);
			final String codeBarre = CODE_DOCUMENT_FOURRE_NEUTRE + ImpressionFourreNeutreHelperImpl.calculCodeBarre(fourre);
			final String attendu = "06014000520100" + String.format("%08d", es.getNumero()) + "0000";
			Assert.assertEquals(attendu, codeBarre);
			return null;
		});
	}
}