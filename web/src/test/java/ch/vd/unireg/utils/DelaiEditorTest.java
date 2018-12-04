package ch.vd.unireg.utils;

import org.junit.Test;

import ch.vd.unireg.type.delai.DelaiComposite;
import ch.vd.unireg.type.delai.DelaiEnJours;
import ch.vd.unireg.type.delai.DelaiEnMois;

import static org.junit.Assert.assertEquals;

public class DelaiEditorTest {

	@Test
	public void testGetAsTextTechnicalFormat() {

		final DelaiEditor editor = new DelaiEditor(true, false, DelaiEditor.Format.TECHNICAL);

		editor.setValue(new DelaiEnJours(3, false));
		assertEquals("3D", editor.getAsText());

		editor.setValue(new DelaiEnJours(3, true));
		assertEquals("3D~", editor.getAsText());

		editor.setValue(new DelaiEnMois(3, false));
		assertEquals("3M", editor.getAsText());

		editor.setValue(new DelaiEnMois(3, true));
		assertEquals("3M~", editor.getAsText());

		editor.setValue(new DelaiComposite(new DelaiEnMois(3, false), new DelaiEnJours(75, false)));
		assertEquals("3M + 75D", editor.getAsText());

		editor.setValue(new DelaiComposite(new DelaiEnMois(3, true), new DelaiEnJours(75, true)));
		assertEquals("3M~ + 75D~", editor.getAsText());
	}

	@Test
	public void testSetAsTextTechnicalFormat() {

		final DelaiEditor editor = new DelaiEditor(true, false, DelaiEditor.Format.TECHNICAL);

		editor.setAsText("3D");
		assertEquals(new DelaiEnJours(3, false), editor.getValue());

		editor.setAsText("3D~");
		assertEquals(new DelaiEnJours(3, true), editor.getValue());

		editor.setAsText("3M");
		assertEquals(new DelaiEnMois(3, false), editor.getValue());

		editor.setAsText("3M~");
		assertEquals(new DelaiEnMois(3, true), editor.getValue());

		editor.setAsText("3M + 75D");
		assertEquals(new DelaiComposite(new DelaiEnMois(3, false), new DelaiEnJours(75, false)), editor.getValue());

		editor.setAsText("3M~ + 75D~");
		assertEquals(new DelaiComposite(new DelaiEnMois(3, true), new DelaiEnJours(75, true)), editor.getValue());
	}

	@Test
	public void testGetAsTextDisplayFormat() {

		final DelaiEditor editor = new DelaiEditor(true, false, DelaiEditor.Format.DISPLAY);

		editor.setValue(new DelaiEnJours(3, false));
		assertEquals("3 jours", editor.getAsText());

		editor.setValue(new DelaiEnJours(3, true));
		assertEquals("3 jours~", editor.getAsText());

		editor.setValue(new DelaiEnMois(3, false));
		assertEquals("3 mois", editor.getAsText());

		editor.setValue(new DelaiEnMois(3, true));
		assertEquals("3 mois~", editor.getAsText());

		editor.setValue(new DelaiComposite(new DelaiEnMois(3, false), new DelaiEnJours(75, false)));
		assertEquals("3 mois + 75 jours", editor.getAsText());

		editor.setValue(new DelaiComposite(new DelaiEnMois(3, true), new DelaiEnJours(75, true)));
		assertEquals("3 mois~ + 75 jours~", editor.getAsText());
	}

	@Test
	public void testSetAsTextDisplayFormat() {

		final DelaiEditor editor = new DelaiEditor(true, false, DelaiEditor.Format.DISPLAY);

		editor.setAsText("3 jours");
		assertEquals(new DelaiEnJours(3, false), editor.getValue());

		editor.setAsText("3      jours");
		assertEquals(new DelaiEnJours(3, false), editor.getValue());

		editor.setAsText("3jours");
		assertEquals(new DelaiEnJours(3, false), editor.getValue());

		editor.setAsText("3 jours~");
		assertEquals(new DelaiEnJours(3, true), editor.getValue());

		editor.setAsText("3 mois");
		assertEquals(new DelaiEnMois(3, false), editor.getValue());

		editor.setAsText("3 mois~");
		assertEquals(new DelaiEnMois(3, true), editor.getValue());

		editor.setAsText("3 mois + 75 jours");
		assertEquals(new DelaiComposite(new DelaiEnMois(3, false), new DelaiEnJours(75, false)), editor.getValue());

		editor.setAsText("3    mois   +   75jours");
		assertEquals(new DelaiComposite(new DelaiEnMois(3, false), new DelaiEnJours(75, false)), editor.getValue());

		editor.setAsText("3 mois~ + 75 jours~");
		assertEquals(new DelaiComposite(new DelaiEnMois(3, true), new DelaiEnJours(75, true)), editor.getValue());
	}
}