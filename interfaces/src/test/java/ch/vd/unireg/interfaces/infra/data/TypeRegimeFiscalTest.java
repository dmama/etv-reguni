package ch.vd.unireg.interfaces.infra.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.fidor.xml.regimefiscal.v2.CategorieEntreprise;
import ch.vd.fidor.xml.regimefiscal.v2.Exoneration;
import ch.vd.fidor.xml.regimefiscal.v2.GenreImpot;
import ch.vd.fidor.xml.regimefiscal.v2.ModeExoneration;
import ch.vd.fidor.xml.regimefiscal.v2.RegimeFiscal;

public class TypeRegimeFiscalTest {

	@Test
	public void testSansExoneration() {
		final RegimeFiscal rf = new RegimeFiscal("7487", "Test", true, false, new CategorieEntreprise("PM", "Personne morale", Collections.emptyList()), null, null, null, null, null, 2015, null);
		final TypeRegimeFiscal type = TypeRegimeFiscalFidor.get(rf);
		Assert.assertNotNull(type);
		Assert.assertEquals("7487", type.getCode());
		Assert.assertEquals("7487 - Test", type.getLibelleAvecCode());
		Assert.assertTrue(type.isCantonal());
		Assert.assertFalse(type.isFederal());
		Assert.assertEquals((Integer) 2015, type.getPremierePeriodeFiscaleValidite());
		Assert.assertNull(type.getDernierePeriodeFiscaleValidite());
		for (int pf = 1990 ; pf < 2100 ; ++ pf) {
			Assert.assertNull(String.valueOf(pf), type.getExonerationIBC(pf));
			Assert.assertNull(type.getExonerationICI(pf));
			Assert.assertNull(type.getExonerationIFONC(pf));
		}
	}

	@Test
	public void testAvecExoneration() {
		final List<Exoneration> exos = Arrays.asList(new Exoneration(2000, 1995, new GenreImpot("IBC", "Impôt fonc.", "Impôt foncier", null), ModeExoneration.EXONERATION_DE_FAIT, null), new Exoneration(null, 2004, new GenreImpot("IBC", "Impôt fonc.", "Impôt foncier", null), ModeExoneration.EXONERATION_DE_FAIT, null));
		final RegimeFiscal rf = new RegimeFiscal("7487", "Test", true, true, new CategorieEntreprise("APM", "Autre personne morale", Collections.emptyList()), exos, null, null, null, null, 1995, null);
		final TypeRegimeFiscal type = TypeRegimeFiscalFidor.get(rf);
		Assert.assertNotNull(type);
		Assert.assertEquals("7487", type.getCode());
		Assert.assertEquals("7487 - Test", type.getLibelleAvecCode());
		Assert.assertTrue(type.isCantonal());
		Assert.assertTrue(type.isFederal());
		Assert.assertEquals((Integer) 1995, type.getPremierePeriodeFiscaleValidite());
		Assert.assertNull(type.getDernierePeriodeFiscaleValidite());
		for (int pf = 1990 ; pf < 2100 ; ++ pf) {
			if ((pf >= 1995 && pf <= 2000) || pf >= 2004) {
				Assert.assertNotNull(String.valueOf(pf), type.getExonerationIBC(pf));
			}
			else {
				Assert.assertNull(String.valueOf(pf), type.getExonerationIBC(pf));
			}
			Assert.assertNull(type.getExonerationICI(pf));
			Assert.assertNull(type.getExonerationIFONC(pf));
		}
	}
}
