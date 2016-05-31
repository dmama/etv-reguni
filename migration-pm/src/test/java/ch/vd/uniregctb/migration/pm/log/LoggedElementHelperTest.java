package ch.vd.uniregctb.migration.pm.log;

import org.junit.Assert;
import org.junit.Test;

public class LoggedElementHelperTest {

	@Test
	public void testCheckValue() throws Exception {

		// MESSAGE -> String
		LoggedElementHelper.checkValue(LoggedElementAttribute.MESSAGE, "Ceci est une chaîne de caractères");
		try {
			LoggedElementHelper.checkValue(LoggedElementAttribute.MESSAGE, 42L);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("La valeur '42' (classe java.lang.Long) n'est pas admise pour le concept MESSAGE", e.getMessage());
		}

		// ENTREPRISE_ID -> Number
		LoggedElementHelper.checkValue(LoggedElementAttribute.ENTREPRISE_ID, 42L);
		LoggedElementHelper.checkValue(LoggedElementAttribute.ENTREPRISE_ID, 42);
		LoggedElementHelper.checkValue(LoggedElementAttribute.ENTREPRISE_ID, 12.01);
		try {
			LoggedElementHelper.checkValue(LoggedElementAttribute.ENTREPRISE_ID, "String...");
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("La valeur 'String...' (classe java.lang.String) n'est pas admise pour le concept ENTREPRISE_ID", e.getMessage());
		}
	}
}
