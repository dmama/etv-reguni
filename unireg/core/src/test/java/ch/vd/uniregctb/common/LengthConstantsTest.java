package ch.vd.uniregctb.common;

import org.junit.Assert;
import org.junit.Test;

public class LengthConstantsTest extends WithoutSpringTest {

	@Test
	public void testNullSteamlining() throws Exception {
		Assert.assertNull(LengthConstants.streamlineField(null, 100, false));
		Assert.assertNull(LengthConstants.streamlineField(null, 100, true));
		Assert.assertNull(LengthConstants.streamlineField("", 100, false));
		Assert.assertNull(LengthConstants.streamlineField("", 100, true));
		Assert.assertNull(LengthConstants.streamlineField(" ", 100, false));
		Assert.assertNull(LengthConstants.streamlineField(" ", 100, true));
		Assert.assertNull(LengthConstants.streamlineField(null, 0, false));
		Assert.assertNull(LengthConstants.streamlineField(null, 0, true));
		Assert.assertNull(LengthConstants.streamlineField("toto", 0, false));
		Assert.assertNull(LengthConstants.streamlineField("toto", 0, true));
	}

	@Test
	public void testTruncatedStreamlining() throws Exception {
		final String src = "0123456789";

		// juste pas tronqué
		Assert.assertEquals(src, LengthConstants.streamlineField(src, src.length(), false));
		Assert.assertEquals(src, LengthConstants.streamlineField(src, src.length(), true));

		// juste pas tronqué après que les espaces initiaux aient été enlevés
		Assert.assertEquals(src, LengthConstants.streamlineField("   " + src, src.length(), false));
		Assert.assertEquals(src, LengthConstants.streamlineField("   " + src, src.length(), true));

		// plus de place que de nécessaire
		Assert.assertEquals(src, LengthConstants.streamlineField(src, src.length() + 1, false));
		Assert.assertEquals(src, LengthConstants.streamlineField(src, src.length() + 1, true));

		// dernier caractère tronqué
		Assert.assertEquals(src.substring(0, src.length() - 1), LengthConstants.streamlineField(src, src.length() - 1, false));
		Assert.assertEquals(src.substring(0, src.length() - 1), LengthConstants.streamlineField(src, src.length() - 1, true));
	}

	@Test
	public void testTrimmingStreamlining() throws Exception {
		Assert.assertEquals("s", LengthConstants.streamlineField("s ", 10, false));
		Assert.assertEquals("s", LengthConstants.streamlineField("s ", 10, true));
		Assert.assertEquals("s", LengthConstants.streamlineField(" s", 10, false));
		Assert.assertEquals("s", LengthConstants.streamlineField(" s", 10, true));
		Assert.assertEquals("s", LengthConstants.streamlineField(" s ", 10, false));
		Assert.assertEquals("s", LengthConstants.streamlineField(" s ", 10, true));
	}

	@Test
	public void testMutipleBlanksStreamlining() throws Exception {
		Assert.assertEquals("2  espaces", LengthConstants.streamlineField("2  espaces", 10, false));
		Assert.assertEquals("2 espaces", LengthConstants.streamlineField("2  espaces", 10, true));
		Assert.assertEquals("2 \tespaces", LengthConstants.streamlineField("2 \tespaces", 10, false));
		Assert.assertEquals("2 espaces", LengthConstants.streamlineField("2 \tespaces", 10, true));

		Assert.assertEquals("1234  789", LengthConstants.streamlineField("1234  7890", 9, false));
		Assert.assertEquals("1234 7890", LengthConstants.streamlineField("1234  7890", 9, true));
		Assert.assertEquals("1234 7890", LengthConstants.streamlineField("1234      7890", 9, true));
		Assert.assertEquals("1234 78 90", LengthConstants.streamlineField("1234      78          90", 10, true));
		Assert.assertEquals("1234 78 9", LengthConstants.streamlineField("1234      78          90", 9, true));
	}
}
