package ch.vd.uniregctb.regimefiscal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ASSOCIATION;

/**
 * @author Raphaël Marmier, 2017-04-21, <raphael.marmier@vd.ch>
 */
public class MockRegimeFiscalServiceConfigurationTest extends WithoutSpringTest {

	@Test
	public void testDummyConfiguration() {
		final MockRegimeFiscalServiceConfiguration dummy = new MockRegimeFiscalServiceConfiguration();

		try {
			dummy.getCodeTypeRegimeFiscal(ASSOCIATION);
			Assert.fail(MockRegimeFiscalServiceConfigurationTest.class.getSimpleName() + ".getCodeTypeRegimeFiscal() doit lever une UnsupportedOperationException !");
		}
		catch (UnsupportedOperationException e) {
			// tout va bien
		}

		try {
			dummy.getCodeTypeRegimeFiscal(ASSOCIATION);
			Assert.fail(MockRegimeFiscalServiceConfigurationTest.class.getSimpleName() + ".getCodeTypeRegimeFiscal() doit lever une UnsupportedOperationException !");
		}
		catch (UnsupportedOperationException e) {
			// tout va bien
		}
	}
}