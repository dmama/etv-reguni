package ch.vd.uniregctb.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Raphaël Marmier, 2017-01-24, <raphael.marmier@vd.ch>
 */
public class LiteralStringHelperTest extends WithoutSpringTest {
	@Test
	public void stripExtraSpacesAndBlanks() throws Exception {
		final String in = "  une chaîne \tde  caractères pour\ntester ";
		final String out = "une chaîne de caractères pour tester";

		Assert.assertEquals(out, LiteralStringHelper.stripExtraSpacesAndBlanks(in));
	}

}