package ch.vd.unireg.common;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class TryWithResourceTest {

	/**
	 * Si ce test ne passe pas, c'est que la gestion d'un try-with-resource sur une resource potentiellement nulle ne fonctionne pas.
	 * (Ce n'est pas forcément très clair dans la documentation du JDK)
	 */
	@Test
	public void testNullResource() throws Exception {
		try (InputStream in = null) {
			Assert.assertNull(in);
		}
	}
}
