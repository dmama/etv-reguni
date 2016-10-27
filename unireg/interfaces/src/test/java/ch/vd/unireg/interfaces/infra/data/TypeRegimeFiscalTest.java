package ch.vd.unireg.interfaces.infra.data;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.fidor.xml.regimefiscal.v1.Exoneration;
import ch.vd.fidor.xml.regimefiscal.v1.RegimeFiscal;

public class TypeRegimeFiscalTest {

	@Test
	public void testSansExoneration() {
		final RegimeFiscal rf = new RegimeFiscal("7487", "Test", true, false, true, false, true, false, null, null, null, null, 2015, null);
		final TypeRegimeFiscal type = TypeRegimeFiscalImpl.get(rf);
		Assert.assertNotNull(type);
		Assert.assertEquals("7487", type.getCode());
		Assert.assertEquals("7487 - Test", type.getLibelle());
		Assert.assertTrue(type.isCantonal());
		Assert.assertFalse(type.isFederal());
		Assert.assertTrue(type.isPourPM());
		Assert.assertFalse(type.isPourAPM());
		Assert.assertTrue(type.isDefaultPourPM());
		Assert.assertFalse(type.isDefaultPourAPM());
		Assert.assertEquals((Integer) 2015, type.getPremierePeriodeFiscaleValidite());
		Assert.assertNull(type.getDernierePeriodeFiscaleValidite());
		for (int pf = 1990 ; pf < 2100 ; ++ pf) {
			Assert.assertFalse(String.valueOf(pf), type.isExoneration(pf));
		}
	}

	@Test
	public void testAvecExoneration() {
		final List<Exoneration> exos = Arrays.asList(new Exoneration(1995, 2000, null), new Exoneration(2004, null, null));
		final RegimeFiscal rf = new RegimeFiscal("7487", "Test", true, true, true, true, false, false, exos, null, null, null, 1995, null);
		final TypeRegimeFiscal type = TypeRegimeFiscalImpl.get(rf);
		Assert.assertNotNull(type);
		Assert.assertEquals("7487", type.getCode());
		Assert.assertEquals("7487 - Test", type.getLibelle());
		Assert.assertTrue(type.isCantonal());
		Assert.assertTrue(type.isFederal());
		Assert.assertTrue(type.isPourPM());
		Assert.assertTrue(type.isPourAPM());
		Assert.assertFalse(type.isDefaultPourPM());
		Assert.assertFalse(type.isDefaultPourAPM());
		Assert.assertEquals((Integer) 1995, type.getPremierePeriodeFiscaleValidite());
		Assert.assertNull(type.getDernierePeriodeFiscaleValidite());
		for (int pf = 1990 ; pf < 2100 ; ++ pf) {
			Assert.assertEquals(String.valueOf(pf), (pf >= 1995 && pf <= 2000) || pf >= 2004, type.isExoneration(pf));
		}
	}
}
