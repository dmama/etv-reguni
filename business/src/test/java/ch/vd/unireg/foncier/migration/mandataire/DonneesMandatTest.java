package ch.vd.unireg.foncier.migration.mandataire;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

public class DonneesMandatTest extends WithoutSpringTest {

	@Test
	public void testParsingChaineVide() {
		try {
			DonneesMandat.valueOf("");
			Assert.fail();      // ne correspond pas du tout à ce qui est attendu...
		}
		catch (ParseException e) {
			// tout va bien...
		}
	}

	@Test
	public void testParsingChaineNulle() {
		try {
			DonneesMandat.valueOf(null);
			Assert.fail();      // ne correspond pas du tout à ce qui est attendu...
		}
		catch (ParseException e) {
			// tout va bien...
		}
	}

	@Test
	public void testParsingChaineValideSansCourrier() throws Exception {
		final String line = "10347562;17779086;Taxation originelle;VAL1;Non;Leurs excellences;Régie Brumont SA;RL;MR Eric Brunisholz, adm.;Grand-Rue 67;1844;Villeneuve;0187148752";
		final DonneesMandat dm = DonneesMandat.valueOf(line);
		Assert.assertNotNull(dm);
		Assert.assertEquals(10347562L, dm.getNoContribuable());
		Assert.assertFalse(dm.isAvecCourrier());
		Assert.assertEquals("Leurs excellences", dm.getFormulePolitesse());
		Assert.assertEquals("Régie Brumont SA", dm.getNom1());
		Assert.assertEquals("RL", dm.getNom2());
		Assert.assertEquals("MR Eric Brunisholz, adm.", dm.getAttentionDe());
		Assert.assertEquals("Grand-Rue 67", dm.getRue());
		Assert.assertEquals((Integer) 1844, dm.getNpa());
		Assert.assertEquals("Villeneuve", dm.getLocalite());
		Assert.assertEquals("0187148752", dm.getNoTelephone());
	}

	@Test
	public void testParsingChaineValideAvecCourrier() throws Exception {
		final String line = "10347562;17779086;Taxation originelle;VAL1;Oui;Leurs excellences;Régie Brumont SA;RL;MR Eric Brunisholz, adm.;Grand-Rue 67;1844;Villeneuve;0187148752";
		final DonneesMandat dm = DonneesMandat.valueOf(line);
		Assert.assertNotNull(dm);
		Assert.assertEquals(10347562L, dm.getNoContribuable());
		Assert.assertTrue(dm.isAvecCourrier());
		Assert.assertEquals("Leurs excellences", dm.getFormulePolitesse());
		Assert.assertEquals("Régie Brumont SA", dm.getNom1());
		Assert.assertEquals("RL", dm.getNom2());
		Assert.assertEquals("MR Eric Brunisholz, adm.", dm.getAttentionDe());
		Assert.assertEquals("Grand-Rue 67", dm.getRue());
		Assert.assertEquals((Integer) 1844, dm.getNpa());
		Assert.assertEquals("Villeneuve", dm.getLocalite());
		Assert.assertEquals("0187148752", dm.getNoTelephone());
	}
}
