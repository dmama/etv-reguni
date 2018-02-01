package ch.vd.uniregctb.wsclient.bvrplus;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class CalculateurChiffreCleTest extends WithoutSpringTest {

	@Test
	public void testExampleLiterature() throws Exception {
		Assert.assertEquals(8, CalculateurChiffreCle.getKey("313947143000901"));
	}
}
