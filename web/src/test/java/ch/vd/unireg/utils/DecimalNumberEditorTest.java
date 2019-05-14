package ch.vd.unireg.utils;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DecimalNumberEditorTest {

	@Test
	public void testGetAsText() throws Exception {

		final DecimalNumberEditor editor = new DecimalNumberEditor(2);

		editor.setValue(null);
		assertEquals(StringUtils.EMPTY, editor.getAsText());

		editor.setValue(BigDecimal.valueOf(120339203, 1));
		assertEquals("12'033'920.30", editor.getAsText());

		editor.setValue(BigDecimal.valueOf(3544, 2));
		assertEquals("35.44", editor.getAsText());
	}

	@Test
	public void testSetAsText() throws Exception {

		final DecimalNumberEditor editor = new DecimalNumberEditor(2);

		editor.setAsText(null);
		assertNull(editor.getValue());

		editor.setAsText("");
		assertNull(editor.getValue());

		editor.setAsText("0");
		assertEquals(BigDecimal.ZERO, editor.getValue());

		editor.setAsText("123.14");
		assertEquals(BigDecimal.valueOf(12314, 2), editor.getValue());

		editor.setAsText("-123.14");
		assertEquals(BigDecimal.valueOf(-12314, 2), editor.getValue());

		editor.setAsText("1'2'0'3'3'9'2'0'3.34");
		assertEquals(BigDecimal.valueOf(12033920334L, 2), editor.getValue());

		try {
			editor.setAsText("1'0dde00.3");
			fail("Pas un nombre, ça aurait dû exploser...");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Cannot parse decimal value: not composed of digits, out of bounds...", e.getMessage());
		}
	}
}
