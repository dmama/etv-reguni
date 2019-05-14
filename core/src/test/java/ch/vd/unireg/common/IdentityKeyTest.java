package ch.vd.unireg.common;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class IdentityKeyTest extends WithoutSpringTest {

	@Test
	public void testEquals() throws Exception {
		final MutableInt one = new MutableInt(42);
		final MutableInt two = new MutableInt(42);

		// the two objects, although not the same, have the same value
		assertEquals(one, two);

		final IdentityKey<MutableInt> oneWrapper = new IdentityKey<>(one);
		final IdentityKey<MutableInt> oneWrapperBis = new IdentityKey<>(one);
		final IdentityKey<MutableInt> twoWrapper = new IdentityKey<>(two);

		// different because the two wrapped objects are not the same
		assertNotEquals(oneWrapper, twoWrapper);
		assertNotEquals(twoWrapper, oneWrapper);

		// equal because the two wrapped objects are the same
		assertEquals(oneWrapper, oneWrapperBis);
		assertEquals(oneWrapperBis, oneWrapper);
	}
}
