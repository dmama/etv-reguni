package ch.vd.uniregctb.common;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CsvHelperTest extends WithoutSpringTest {

	@Test
	public void testEscapeChars() throws Exception {
		assertEquals(StringUtils.EMPTY, CsvHelper.escapeChars(null));
		assertEquals(StringUtils.EMPTY, CsvHelper.escapeChars(""));
		assertEquals(StringUtils.EMPTY, CsvHelper.escapeChars("  "));
		assertEquals(StringUtils.EMPTY, CsvHelper.escapeChars("\t"));
		assertEquals(StringUtils.EMPTY, CsvHelper.escapeChars("\";\t  ;\""));
		assertEquals(StringUtils.EMPTY, CsvHelper.escapeChars("\u00a0"));       // non-breakable space

		assertEquals("David Vincent", CsvHelper.escapeChars("David\tVincent"));
		assertEquals("David Vincent", CsvHelper.escapeChars("David\u00a0Vincent "));
	}
}
