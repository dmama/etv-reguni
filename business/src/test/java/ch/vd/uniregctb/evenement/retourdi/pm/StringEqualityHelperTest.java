package ch.vd.uniregctb.evenement.retourdi.pm;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class StringEqualityHelperTest extends WithoutSpringTest {

	@Test
	public void testEqualityEmptyNull() throws Exception {
		Assert.assertTrue(StringEqualityHelper.equals(null, null));
		Assert.assertTrue(StringEqualityHelper.equals("", "  "));
		Assert.assertTrue(StringEqualityHelper.equals("  ", ""));
		Assert.assertFalse(StringEqualityHelper.equals(null, "  "));
		Assert.assertFalse(StringEqualityHelper.equals("  ", null));
	}

	@Test
	public void testEqualityNull() throws Exception {
		Assert.assertTrue(StringEqualityHelper.equals(null, null));
		Assert.assertFalse(StringEqualityHelper.equals(null, "toto"));
		Assert.assertFalse(StringEqualityHelper.equals("toto", null));
		Assert.assertTrue(StringEqualityHelper.equals("toto", "toto"));
	}

	@Test
	public void testEqualityTrim() throws Exception {
		Assert.assertTrue(StringEqualityHelper.equals("toto", " toto  "));
		Assert.assertTrue(StringEqualityHelper.equals("  toto ", "toto"));
		Assert.assertTrue(StringEqualityHelper.equals("  toto      ", "     toto "));
	}

	@Test
	public void testEqualityAccentsAndLowecase() throws Exception {
		Assert.assertTrue(StringEqualityHelper.equals("Zürich", "zuRIch"));
		Assert.assertTrue(StringEqualityHelper.equals("zuRIch", "Zürich"));
		Assert.assertTrue(StringEqualityHelper.equals("Genève", "genÈVE"));
		Assert.assertTrue(StringEqualityHelper.equals("genÈVE", "Genève"));
	}
}
