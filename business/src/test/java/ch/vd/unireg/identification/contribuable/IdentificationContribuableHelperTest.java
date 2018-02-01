package ch.vd.uniregctb.identification.contribuable;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IdentificationContribuableHelperTest extends WithoutSpringTest {

	@Test
	public void testGetSansDernierMot() throws Exception {
		assertNull(IdentificationContribuableHelper.sansDernierMot("jean-rené", false));
		assertEquals("jean-rené", IdentificationContribuableHelper.sansDernierMot("jean-rené albert", false));
		assertEquals("albert", IdentificationContribuableHelper.sansDernierMot("albert jean-rené", false));
		assertEquals("jean", IdentificationContribuableHelper.sansDernierMot("jean rené", false));
		assertEquals("jean rené", IdentificationContribuableHelper.sansDernierMot("jean rené albert", false));
		assertNull(IdentificationContribuableHelper.sansDernierMot(" ", false));
		assertNull(IdentificationContribuableHelper.sansDernierMot("", false));

		assertNull(IdentificationContribuableHelper.sansDernierMot("jean", true));
		assertEquals("jean", IdentificationContribuableHelper.sansDernierMot("jean-rené", true));
		assertEquals("jean-rené", IdentificationContribuableHelper.sansDernierMot("jean-rené albert", true));
		assertEquals("albert", IdentificationContribuableHelper.sansDernierMot("albert jean-rené", true));
		assertEquals("jean", IdentificationContribuableHelper.sansDernierMot("jean rené", true));
		assertEquals("jean rené", IdentificationContribuableHelper.sansDernierMot("jean rené albert", true));
		assertNull(IdentificationContribuableHelper.sansDernierMot(" ", true));
		assertNull(IdentificationContribuableHelper.sansDernierMot("", true));
	}

	@Test
	public void testGetMotSansE() throws Exception {
		// a, o, u, mais pas i ni e...
		assertEquals("muller", IdentificationContribuableHelper.getMotSansE("MUeller"));
		assertEquals("moler", IdentificationContribuableHelper.getMotSansE("MoEler"));
		assertEquals("mahler", IdentificationContribuableHelper.getMotSansE("MaeHler"));
		assertEquals("miele", IdentificationContribuableHelper.getMotSansE("Miele"));
		assertEquals("schnee", IdentificationContribuableHelper.getMotSansE("schnee"));
	}
}
