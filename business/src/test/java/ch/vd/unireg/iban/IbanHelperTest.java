package ch.vd.unireg.iban;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

public class IbanHelperTest extends WithoutSpringTest {

	@Test
	public void testNormalize() {
		Assert.assertNull(IbanHelper.normalize(null));
		Assert.assertNull(IbanHelper.normalize("    --/"));
		Assert.assertEquals("CH9308440717427290198", IbanHelper.normalize(" ch 93084 407174 2729 019 8"));
	}

	@Test
	public void testToDisplayString() {
		Assert.assertEquals(null, IbanHelper.toDisplayString(null));
		Assert.assertEquals("TORTILLA", IbanHelper.toDisplayString("TORTILLA"));
		Assert.assertEquals("CH93084407174272901983", IbanHelper.toDisplayString("CH93084407174272901983"));    // invalide : un caractère en trop
		Assert.assertEquals("CH93 0844 0717 4272 9019 8", IbanHelper.toDisplayString("CH9308440717427290198"));
	}

	@Test
	public void testAreSame() {
		Assert.assertTrue(IbanHelper.areSame(null, null));
		Assert.assertTrue(IbanHelper.areSame("   ", null));
		Assert.assertTrue(IbanHelper.areSame("CH9308440717427290198", " ch 93084 407174 2729 019 8"));
		Assert.assertTrue(IbanHelper.areSame("CH93084407174272901983", " ch 93084 407174 2729 019 83"));       // même invalide, on normalize

		Assert.assertFalse(IbanHelper.areSame("CH9308440717427290198", null));
		Assert.assertFalse(IbanHelper.areSame("CH93084407174272901983", " ch 93084 407174 2729 019 8"));
	}
}
