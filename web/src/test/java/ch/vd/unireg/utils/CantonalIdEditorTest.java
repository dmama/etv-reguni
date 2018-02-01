package ch.vd.unireg.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CantonalIdEditorTest {

	@Test
	public void testGetAsText() throws Exception {

		final CantonalIdEditor editor = new CantonalIdEditor();

		editor.setValue(null);
		assertEquals("", editor.getAsText());

		editor.setValue(120339203);     // int
		assertEquals("120-339-203", editor.getAsText());

		editor.setValue(120339203L);    // long
		assertEquals("120-339-203", editor.getAsText());
	}

	@Test
	public void testSetAsText() throws Exception {

		final CantonalIdEditor editor = new CantonalIdEditor();

		editor.setAsText(null);
		assertNull(editor.getValue());

		editor.setAsText("");
		assertNull(editor.getValue());

		editor.setAsText("120339203");
		assertEquals(120339203L, editor.getValue());

		editor.setAsText("120-339-287");
		assertEquals(120339287L, editor.getValue());
	}
}