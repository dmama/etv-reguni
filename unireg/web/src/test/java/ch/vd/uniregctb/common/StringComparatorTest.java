package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class StringComparatorTest extends WithoutSpringTest {

	@Test
	public void testRemoveAccents() throws Exception {
		Assert.assertNull(StringComparator.removeAccents(null));
		Assert.assertEquals("", StringComparator.removeAccents(""));
		Assert.assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ -?1234567890~!$£§°+*%&/()=?", StringComparator.removeAccents("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ -?1234567890~!$£§°+*%&/()=?"));
		Assert.assertEquals("aaaaaaAAAAAA", StringComparator.removeAccents("áàâäãåÁÀÂÄÃÅ"));
		Assert.assertEquals("aeAE", StringComparator.removeAccents("æÆ"));
		Assert.assertEquals("cC", StringComparator.removeAccents("çÇ"));
		Assert.assertEquals("dD", StringComparator.removeAccents("ðÐ"));
		Assert.assertEquals("eeeeeEEEEE", StringComparator.removeAccents("éèêëẽÉÈÊËẼ"));
		Assert.assertEquals("iiiiiIIIII", StringComparator.removeAccents("íìîïĩÍÌÎÏĨ"));
		Assert.assertEquals("lL", StringComparator.removeAccents("łŁ"));
		Assert.assertEquals("nN", StringComparator.removeAccents("ñÑ"));
		Assert.assertEquals("ooooooOOOOOO", StringComparator.removeAccents("óòôöõøÓÒÔÖÕØ"));
		Assert.assertEquals("oeOE", StringComparator.removeAccents("œŒ"));
		Assert.assertEquals("pP", StringComparator.removeAccents("Þþ"));
		Assert.assertEquals("sSss", StringComparator.removeAccents("šŠß"));
		Assert.assertEquals("uuuuuUUUUU", StringComparator.removeAccents("úùûüũÚÙÛÜŨ"));
		Assert.assertEquals("yyyyyYYYYY", StringComparator.removeAccents("ýỳŷÿỹÝỲŶŸỸ"));
		Assert.assertEquals("zZ", StringComparator.removeAccents("žŽ"));
	}

	@Test
	public void testCaseSentitiveness() throws Exception {

		// non-accent sensitive
		{
			final Comparator<String> caseSensitive = new StringComparator(false, true, true, null);
			final Comparator<String> caseInsensitive = new StringComparator(false, false, true, null);

			Assert.assertEquals(0, caseInsensitive.compare("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz"));
			Assert.assertEquals(0, caseInsensitive.compare("abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
			Assert.assertTrue(caseSensitive.compare("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz") < 0);
			Assert.assertTrue(caseSensitive.compare("abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ") > 0);
		}

		// accent sensitive
		{
			final Comparator<String> caseSensitive = new StringComparator(true, true, true, null);
			final Comparator<String> caseInsensitive = new StringComparator(true, false, true, null);

			Assert.assertEquals(0, caseInsensitive.compare("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz"));
			Assert.assertEquals(0, caseInsensitive.compare("abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
			Assert.assertTrue(caseSensitive.compare("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz") < 0);
			Assert.assertTrue(caseSensitive.compare("abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ") > 0);
		}
	}

	@Test
	public void testAccentSensitiveness() throws Exception {
		final Comparator<String> sensitive = new StringComparator(true, true, true, null);
		final Comparator<String> insensitive = new StringComparator(false, true, true, null);

		Assert.assertEquals(0, insensitive.compare("ageuukwsavuzfgavzfw", "ägẽuùkwsâvûzfgåvžfw"));
		Assert.assertEquals(0, insensitive.compare("ägẽuùkwsâvûzfgåvžfw", "ageuukwsavuzfgavzfw"));
		Assert.assertTrue(sensitive.compare("ageuukwsavuzfgavzfw", "ägẽuùkwsâvûzfgåvžfw") < 0);
		Assert.assertTrue(sensitive.compare("ägẽuùkwsâvûzfgåvžfw", "ageuukwsavuzfgavzfw") > 0);
	}

	@Test
	public void testDecodage() throws Exception {

		// décoder qui fait passer en minuscules
		final StringComparator.Decoder lowerCaseDecoder = new StringComparator.Decoder() {
			public String decode(String source) {
				return StringUtils.lowerCase(source);
			}
		};

		final StringComparator comparateur = new StringComparator(false, true, true, lowerCaseDecoder);

		final String s1 = "AeIoUY";
		final String s2 = "aEiOuy";
		Assert.assertEquals(0, comparateur.compare(s1, s2));
		Assert.assertEquals(0, comparateur.compare(s2, s1));
	}

	@Test
	public void testDecodageXml() throws Exception {

		// c'est le cas qui nous intéresse réellement, en fait

		final StringComparator.Decoder decoder = new StringComparator.Decoder() {
			public String decode(String source) {
				return StringEscapeUtils.unescapeXml(source);
			}
		};

		final StringComparator comparateur = new StringComparator(false, false, true, decoder);

		final String s1 = "Îles Féroé";
		final String s2 = "&#206;les F&#233;ro&#233;";
		Assert.assertEquals(0, comparateur.compare(s1, s2));
		Assert.assertEquals(0, comparateur.compare(s2, s1));
	}

	@Test
	public void testNullPosition() throws Exception {

		{
			final StringComparator nullBefore = new StringComparator(true, true, true, null);
			final List<String> list = new ArrayList<String>(Arrays.asList("otto", null, "itti"));
			Collections.sort(list, nullBefore);
			Assert.assertEquals(3, list.size());
			Assert.assertNull(list.get(0));
			Assert.assertEquals("itti", list.get(1));
			Assert.assertEquals("otto", list.get(2));
		}

		{
			final StringComparator nullAfter = new StringComparator(true, true, false, null);
			final List<String> list = new ArrayList<String>(Arrays.asList("otto", null, "itti"));
			Collections.sort(list, nullAfter);
			Assert.assertEquals(3, list.size());
			Assert.assertEquals("itti", list.get(0));
			Assert.assertEquals("otto", list.get(1));
			Assert.assertNull(list.get(2));
		}
	}
}
