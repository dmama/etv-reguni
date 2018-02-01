package ch.vd.unireg.interfaces.organisation.data;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

/**
 * @author Raphaël Marmier, 2016-08-24, <raphael.marmier@vd.ch>
 */
public class NumeroIDETest extends WithoutSpringTest {

	@Test
	public void testCreation() throws Exception {

		Assert.assertEquals("CHE999999998", new NumeroIDE("CHE999999998").getValeur());
	}

	@Test
	public void testCreationSansChe() throws Exception {

		Assert.assertEquals("CHE999999998", new NumeroIDE("999999998").getValeur());
	}

	@Test
	public void testValueOf() throws Exception {

		Assert.assertEquals("CHE999999998", NumeroIDE.valueOf(999999998).getValeur());
	}

	@Test
	public void testToString() throws Exception {

		Assert.assertEquals("CHE-999.999.998", new NumeroIDE("CHE999999998").toString());
	}

	@Test
	public void testFailBadNumber() throws Exception {

		try {
			new NumeroIDE("9999998");

			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("CHE9999998 n'est pas un numéro IDE préfixé valide!", e.getMessage());
		}

		try {
			new NumeroIDE("9993249998");

			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("CHE9993249998 n'est pas un numéro IDE préfixé valide!", e.getMessage());
		}

		try {
			new NumeroIDE("999AB9F98");

			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("CHE999AB9F98 n'est pas un numéro IDE préfixé valide!", e.getMessage());
		}
	}

	@Test
	public void testGetValeurBrute() throws Exception {

		Assert.assertEquals(999999998, new NumeroIDE("CHE999999998").getValeurBrute());
	}
}