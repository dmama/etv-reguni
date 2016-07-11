package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class CompositeLoggedElementTest {

	@Test
	public void testCompositionElementsDisjoints() throws Exception {
		final LoggedElement le1 = new SimpleLoggedElement<>(LoggedElementAttribute.MESSAGE, "Mon message");
		final LoggedElement le2 = new SimpleLoggedElement<>(LoggedElementAttribute.ENTREPRISE_ID, 125L);
		final LoggedElement le3 = new SimpleLoggedElement<>(LoggedElementAttribute.ETABLISSEMENT_ID, 1542L);

		final LoggedElement composite = new CompositeLoggedElement(le1, le2, le3);
		Assert.assertEquals(Arrays.asList(LoggedElementAttribute.MESSAGE, LoggedElementAttribute.ENTREPRISE_ID, LoggedElementAttribute.ETABLISSEMENT_ID), composite.getItems());

		final Map<LoggedElementAttribute, Object> itemValues = composite.getItemValues();
		Assert.assertEquals("Mon message", itemValues.get(LoggedElementAttribute.MESSAGE));
		Assert.assertEquals(125L, itemValues.get(LoggedElementAttribute.ENTREPRISE_ID));
		Assert.assertEquals(1542L, itemValues.get(LoggedElementAttribute.ETABLISSEMENT_ID));
	}

	@Test
	public void testCompositionAvecElementsCommuns() throws Exception {
		final LoggedElement le1 = new SimpleLoggedElement<>(LoggedElementAttribute.MESSAGE, "Mon message");
		final LoggedElement le2 = new SimpleLoggedElement<>(LoggedElementAttribute.ENTREPRISE_ID, 125L);
		final LoggedElement le3 = new SimpleLoggedElement<>(LoggedElementAttribute.ENTREPRISE_ID, 125L);

		final LoggedElement composite = new CompositeLoggedElement(le1, le2, le3);
		Assert.assertEquals(Arrays.asList(LoggedElementAttribute.MESSAGE, LoggedElementAttribute.ENTREPRISE_ID), composite.getItems());

		final Map<LoggedElementAttribute, Object> itemValues = composite.getItemValues();
		Assert.assertEquals("Mon message", itemValues.get(LoggedElementAttribute.MESSAGE));
		Assert.assertEquals(125L, itemValues.get(LoggedElementAttribute.ENTREPRISE_ID));
	}

	@Test
	public void testCompositionAvecElementsCommunsConcatenables() throws Exception {
		final LoggedElement le1 = new SimpleLoggedElement<>(LoggedElementAttribute.MESSAGE, "Mon message");
		final LoggedElement le2 = new SimpleLoggedElement<>(LoggedElementAttribute.ENTREPRISE_ID, 125L);
		final LoggedElement le3 = new SimpleLoggedElement<>(LoggedElementAttribute.MESSAGE, "Et maintenant le mien");

		final LoggedElement composite = new CompositeLoggedElement(le1, le2, le3);
		Assert.assertEquals(Arrays.asList(LoggedElementAttribute.MESSAGE, LoggedElementAttribute.ENTREPRISE_ID), composite.getItems());

		final Map<LoggedElementAttribute, Object> itemValues = composite.getItemValues();
		Assert.assertEquals("Mon message // Et maintenant le mien", itemValues.get(LoggedElementAttribute.MESSAGE));
		Assert.assertEquals(125L, itemValues.get(LoggedElementAttribute.ENTREPRISE_ID));
	}

	@Test
	public void testCompositionAvecElementsCommunsNonConcatenables() throws Exception {
		final LoggedElement le1 = new SimpleLoggedElement<>(LoggedElementAttribute.MESSAGE, "Mon message");
		final LoggedElement le2 = new SimpleLoggedElement<>(LoggedElementAttribute.ENTREPRISE_ID, 125L);
		final LoggedElement le3 = new SimpleLoggedElement<>(LoggedElementAttribute.ENTREPRISE_ID, 42L);

		final LoggedElement composite = new CompositeLoggedElement(le1, le2, le3);
		try {
			composite.getItemValues();
			Assert.fail();
		}
		catch (LoggedElementHelper.IncompabibleValuesException e) {
			Assert.assertEquals("Concept ENTREPRISE_ID: Valeurs incompatibles 125 // 42", e.getMessage());
		}
	}

}
