package ch.vd.uniregctb.common;

import org.junit.Assert;
import org.junit.Test;

public class EncodingFixHelperTest extends WithoutSpringTest {

	@Test
	public void testFixFromIso() throws Exception {
		final String wrong = "ZÃ¼rich";
		final String right = "Zürich";
		Assert.assertEquals(right, EncodingFixHelper.fixFromIso(wrong));
	}

	@Test
	public void testBreakToIso() throws Exception {
		final String right = "Zürich Genève Délémont";
		final String wrong = "ZÃ¼rich GenÃ¨ve DÃ©lÃ©mont";
		Assert.assertEquals(wrong, EncodingFixHelper.breakToIso(right));
	}
}
