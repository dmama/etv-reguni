package ch.vd.uniregctb.etiquette;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class DecalageTest extends WithoutSpringTest {

	@Test
	public void testDecalage() throws Exception {
		final Decalage decalage = new Decalage(5, UniteDecalageDate.JOUR);
		final RegDate dateDepart = date(2000, 1, 1);
		Assert.assertNull(decalage.apply(null));
		for (int index = 0 ; index < 1000 ; ++ index) {
			final RegDate dateReference = dateDepart.addDays(index);
			final RegDate expected = dateReference.addDays(5);
			Assert.assertEquals(expected, decalage.apply(dateReference));
		}
	}
}
