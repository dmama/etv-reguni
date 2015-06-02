package ch.vd.uniregctb.common;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

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

	@Test
	public void testOutputEncoding() throws Exception {
		assertEquals("ISO-8859-15", CsvHelper.CHARSET);

		final byte[] bytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
		     OutputStreamWriter ows = new OutputStreamWriter(baos, CsvHelper.CHARSET);
			 BufferedWriter bw = new BufferedWriter(ows)) {

			bw.write("æ€ŠšŽžŒœŸ");
			bw.flush();
			bytes = baos.toByteArray();
		}

		// les valeurs pour l'encodage ISO-8859-15 ont été reprises de http://fr.wikipedia.org/wiki/ISO/CEI_8859-15
		assertEquals(0xe6, bytes[0] & 0xff);
		assertEquals(0xa4, bytes[1] & 0xff);
		assertEquals(0xa6, bytes[2] & 0xff);
		assertEquals(0xa8, bytes[3] & 0xff);
		assertEquals(0xb4, bytes[4] & 0xff);
		assertEquals(0xb8, bytes[5] & 0xff);
		assertEquals(0xbc, bytes[6] & 0xff);
		assertEquals(0xbd, bytes[7] & 0xff);
		assertEquals(0xbe, bytes[8] & 0xff);

		// et le retour en string
		final String retour = new String(bytes, CsvHelper.CHARSET);
		assertEquals("æ€ŠšŽžŒœŸ", retour);
	}
}
