package ch.vd.uniregctb.migration.pm.log;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.migration.pm.regpm.NumeroIDE;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;

public class LoggedElementRendererTest {

	@Test
	public void testEntrepriseFullLoggedElement() {
		final RegpmEntreprise e = new RegpmEntreprise();
		e.setId(42L);
		e.setNumeroCantonal(23687342L);

		final NumeroIDE ide = new NumeroIDE();
		ide.setCategorie("CHE");
		ide.setNumero(123456789L);
		e.setNumeroIDE(ide);

		final EntrepriseLoggedElement logged = new EntrepriseLoggedElement(e, true);
		Assert.assertEquals("42;Active;CHE123456789;23687342", LoggedElementRenderer.INSTANCE.toString(logged));
	}

	@Test
	public void testEntrepriseLoggedElementWithMissingParts() {
		final RegpmEntreprise e = new RegpmEntreprise();
		e.setId(42L);
		e.setNumeroCantonal(23687342L);

		final EntrepriseLoggedElement logged = new EntrepriseLoggedElement(e, false);
		Assert.assertEquals("42;Inactive;;23687342", LoggedElementRenderer.INSTANCE.toString(logged));
	}

	@Test
	public void testComposition() {
		final LoggedElement one = new SimpleLoggedElement<>(LoggedElementAttribute.ENTREPRISE_ID, 12344L);
		final LoggedElement two = new SimpleLoggedElement<>(LoggedElementAttribute.MESSAGE, "Ceci est mon entreprise...");

		final LoggedElement composite1 = CompositeLoggedElement.compose(one, two);
		Assert.assertEquals("12344;Ceci est mon entreprise...", LoggedElementRenderer.INSTANCE.toString(composite1));

		final LoggedElement composite2 = CompositeLoggedElement.compose(two, one);
		Assert.assertEquals("Ceci est mon entreprise...;12344", LoggedElementRenderer.INSTANCE.toString(composite2));
	}
}
