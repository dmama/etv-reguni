package ch.vd.unireg.common;

import org.junit.Assert;
import org.junit.Test;

public class IdentityKeyTest extends WithoutSpringTest {

	@Test
	public void testEquals() throws Exception {
		final Integer one = new Integer(42);
		final Integer two = new Integer(42);

		// the two objects, although not the same, have the same value
		Assert.assertEquals(one, two);

		final IdentityKey<Integer> oneWrapper = new IdentityKey<>(one);
		final IdentityKey<Integer> oneWrapperBis = new IdentityKey<>(one);
		final IdentityKey<Integer> twoWrapper = new IdentityKey<>(two);

		// different because the two wrapped objects are not the same
		Assert.assertFalse(oneWrapper.equals(twoWrapper));
		Assert.assertFalse(twoWrapper.equals(oneWrapper));

		// equal because the two wrapped objects are the same
		Assert.assertEquals(oneWrapper, oneWrapperBis);
		Assert.assertEquals(oneWrapperBis, oneWrapper);
	}
}
