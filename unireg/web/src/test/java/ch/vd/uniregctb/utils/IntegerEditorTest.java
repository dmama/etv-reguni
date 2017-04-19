package ch.vd.uniregctb.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IntegerEditorTest {

	@Test
	public void testGetAsText() throws Exception {

		final IntegerEditor editor = new IntegerEditor(true);

		editor.setValue(null);
		assertEquals(StringUtils.EMPTY, editor.getAsText());

		editor.setValue(120339203);     // int
		assertEquals("120'339'203", editor.getAsText());

		editor.setValue(120339203L);    // long
		assertEquals("120'339'203", editor.getAsText());
	}

	@Test
	public void testSetAsTextPositiveOnly() throws Exception {

		final IntegerEditor editor = new IntegerEditor(true);

		editor.setAsText(null);
		assertNull(editor.getValue());

		editor.setAsText("");
		assertNull(editor.getValue());

		editor.setAsText("0");
		assertEquals(0, editor.getValue());

		editor.setAsText("123");
		assertEquals(123, editor.getValue());

		editor.setAsText("1'2'0'3'3'9'2'0'3");
		assertEquals(120339203, editor.getValue());

		editor.setAsText("120'339'287");
		assertEquals(120339287, editor.getValue());

		try {
			editor.setAsText("-1'000");
			fail("Nombre négatif, ça aurait dû exploser...");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Cannot parse integer value: not composed of digits, out of bounds...", e.getMessage());
		}

		try {
			editor.setAsText("1'0dde00");
			fail("Pas un nombre, ça aurait dû exploser...");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Cannot parse integer value: not composed of digits, out of bounds...", e.getMessage());
		}
	}

	@Test
	public void testSetAsTextNotPositiveOnly() throws Exception {

		final IntegerEditor editor = new IntegerEditor(false);

		editor.setAsText(null);
		assertNull(editor.getValue());

		editor.setAsText("");
		assertNull(editor.getValue());

		editor.setAsText("0");
		assertEquals(0, editor.getValue());

		editor.setAsText("123");
		assertEquals(123, editor.getValue());

		editor.setAsText("1'2'0'3'3'9'2'0'3");
		assertEquals(120339203, editor.getValue());

		editor.setAsText("120'339'287");
		assertEquals(120339287, editor.getValue());

		editor.setAsText("-1'000");
		assertEquals(-1000, editor.getValue());

		try {
			editor.setAsText("1'0dde00");
			fail("Pas un nombre, ça aurait dû exploser...");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Cannot parse integer value: not composed of digits, out of bounds...", e.getMessage());
		}
	}

}
