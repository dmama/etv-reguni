package ch.vd.uniregctb.migration.pm.engine.log;

import java.util.Collections;
import java.util.EmptyStackException;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.migration.pm.log.LoggedElementAttribute;
import ch.vd.uniregctb.migration.pm.log.SimpleLoggedElement;

public class LogContexteTest {

	@Test
	public void testPushPop() throws Exception {
		final LogContexte contexte = new LogContexte();

		// une première valeur
		contexte.pushContextValue(SimpleLoggedElement.class, new SimpleLoggedElement<>(LoggedElementAttribute.ENTREPRISE_ID, 42L));
		{
			final SimpleLoggedElement value = contexte.getContextValue(SimpleLoggedElement.class);
			Assert.assertNotNull(value);
			Assert.assertEquals(Collections.singletonList(LoggedElementAttribute.ENTREPRISE_ID), value.getItems());
			Assert.assertEquals(Collections.singletonMap(LoggedElementAttribute.ENTREPRISE_ID, 42L), value.getItemValues());
		}

		// une seconde valeur
		contexte.pushContextValue(SimpleLoggedElement.class, new SimpleLoggedElement<>(LoggedElementAttribute.ETABLISSEMENT_ID, 256L));
		{
			final SimpleLoggedElement value = contexte.getContextValue(SimpleLoggedElement.class);
			Assert.assertNotNull(value);
			Assert.assertEquals(Collections.singletonList(LoggedElementAttribute.ETABLISSEMENT_ID), value.getItems());
			Assert.assertEquals(Collections.singletonMap(LoggedElementAttribute.ETABLISSEMENT_ID, 256L), value.getItemValues());
		}

		// premier retour en arrière (-> retour à la première valeur)
		contexte.popContextValue(SimpleLoggedElement.class);
		{
			final SimpleLoggedElement value = contexte.getContextValue(SimpleLoggedElement.class);
			Assert.assertNotNull(value);
			Assert.assertEquals(Collections.singletonList(LoggedElementAttribute.ENTREPRISE_ID), value.getItems());
			Assert.assertEquals(Collections.singletonMap(LoggedElementAttribute.ENTREPRISE_ID, 42L), value.getItemValues());
		}

		// deuxième retour en arrière (-> retour à "vide")
		contexte.popContextValue(SimpleLoggedElement.class);
		Assert.assertNull(contexte.getContextValue(SimpleLoggedElement.class));

		// un pop de trop -> exception
		try {
			contexte.popContextValue(SimpleLoggedElement.class);
			Assert.fail("Ce pop est de trop, on aurait dû le voir...");
		}
		catch (EmptyStackException e) {
			// alors tout va bien, on a vu le souci...
		}

	}
}
