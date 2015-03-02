package ch.vd.unireg.servlet.security;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemoteHostSpringFilterTest {

	@Test
	public void testWildcardToRegExp() {
		assertEquals("10\\.240\\.6\\.177", RemoteHostSpringFilter.wildcardToRegExp("10.240.6.177"));
		assertEquals("10\\.240\\.6\\.[.0-9]+", RemoteHostSpringFilter.wildcardToRegExp("10.240.6.*"));
		assertEquals("10\\.240\\.[.0-9]+", RemoteHostSpringFilter.wildcardToRegExp("10.240.*"));
		assertEquals("10\\.[.0-9]+", RemoteHostSpringFilter.wildcardToRegExp("10.*"));
		assertEquals("[.0-9]+", RemoteHostSpringFilter.wildcardToRegExp("*"));
	}

	@Test
	public void testIsInvalidAddressDeniedCompleteAddress() {

		final RemoteHostSpringFilter filter = new RemoteHostSpringFilter();
		filter.setDenied("10.240.6.177");
		filter.setAllowed("");

		assertTrue(filter.isInvalidAddress("10.240.6.177"));
		assertFalse(filter.isInvalidAddress("10.240.6.178"));
		assertFalse(filter.isInvalidAddress("10.240.5.177"));
		assertFalse(filter.isInvalidAddress("09.240.6.177"));
	}

	@Test
	public void testIsInvalidAddressDeniedWildcardAddress() {

		final RemoteHostSpringFilter filter = new RemoteHostSpringFilter();

		filter.setDenied("10.240.6.*");
		filter.setAllowed("");
		assertTrue(filter.isInvalidAddress("10.240.6.177"));
		assertTrue(filter.isInvalidAddress("10.240.6.178"));
		assertFalse(filter.isInvalidAddress("10.240.5.177"));
		assertFalse(filter.isInvalidAddress("09.240.6.177"));

		filter.setDenied("10.240.*");
		filter.setAllowed("");
		assertTrue(filter.isInvalidAddress("10.240.6.177"));
		assertTrue(filter.isInvalidAddress("10.240.6.178"));
		assertTrue(filter.isInvalidAddress("10.240.5.177"));
		assertFalse(filter.isInvalidAddress("09.240.6.177"));

		filter.setDenied("*");
		filter.setAllowed("");
		assertTrue(filter.isInvalidAddress("10.240.6.177"));
		assertTrue(filter.isInvalidAddress("10.240.6.178"));
		assertTrue(filter.isInvalidAddress("10.240.5.177"));
		assertTrue(filter.isInvalidAddress("09.240.6.177"));
	}

	@Test
	public void testIsInvalidAddressDeniedVariousAddresses() {

		final RemoteHostSpringFilter filter = new RemoteHostSpringFilter();

		filter.setDenied("10.240.6.*,196.*,23.45.67.89");
		filter.setAllowed("");
		assertTrue(filter.isInvalidAddress("10.240.6.177"));
		assertTrue(filter.isInvalidAddress("10.240.6.178"));
		assertTrue(filter.isInvalidAddress("196.0.1.23"));
		assertTrue(filter.isInvalidAddress("196.196.196.33"));
		assertTrue(filter.isInvalidAddress("23.45.67.89"));
		assertFalse(filter.isInvalidAddress("10.240.5.177"));
		assertFalse(filter.isInvalidAddress("195.21.5.65"));
		assertFalse(filter.isInvalidAddress("22.45.67.89"));
	}

	@Test
	public void testIsInvalidAddressAllowedCompleteAddress() {

		final RemoteHostSpringFilter filter = new RemoteHostSpringFilter();
		filter.setDenied("");
		filter.setAllowed("10.240.6.177");

		assertFalse(filter.isInvalidAddress("10.240.6.177"));
		assertTrue(filter.isInvalidAddress("10.240.6.178"));
		assertTrue(filter.isInvalidAddress("10.240.5.177"));
		assertTrue(filter.isInvalidAddress("09.240.6.177"));
	}

	@Test
	public void testIsInvalidAddressAllowedWildcardAddress() {

		final RemoteHostSpringFilter filter = new RemoteHostSpringFilter();

		filter.setDenied("");
		filter.setAllowed("10.240.6.*");
		assertFalse(filter.isInvalidAddress("10.240.6.177"));
		assertFalse(filter.isInvalidAddress("10.240.6.178"));
		assertTrue(filter.isInvalidAddress("10.240.5.177"));
		assertTrue(filter.isInvalidAddress("09.240.6.177"));

		filter.setDenied("");
		filter.setAllowed("10.240.*");
		assertFalse(filter.isInvalidAddress("10.240.6.177"));
		assertFalse(filter.isInvalidAddress("10.240.6.178"));
		assertFalse(filter.isInvalidAddress("10.240.5.177"));
		assertTrue(filter.isInvalidAddress("09.240.6.177"));

		filter.setDenied("");
		filter.setAllowed("*");
		assertFalse(filter.isInvalidAddress("10.240.6.177"));
		assertFalse(filter.isInvalidAddress("10.240.6.178"));
		assertFalse(filter.isInvalidAddress("10.240.5.177"));
		assertFalse(filter.isInvalidAddress("09.240.6.177"));
	}

	@Test
	public void testIsInvalidAddressAllowedVariousAddresses() {
		
		final RemoteHostSpringFilter filter = new RemoteHostSpringFilter();

		filter.setDenied("");
		filter.setAllowed("10.240.6.*,196.*,23.45.67.89");
		assertFalse(filter.isInvalidAddress("10.240.6.177"));
		assertFalse(filter.isInvalidAddress("10.240.6.178"));
		assertFalse(filter.isInvalidAddress("196.0.1.23"));
		assertFalse(filter.isInvalidAddress("196.196.196.33"));
		assertFalse(filter.isInvalidAddress("23.45.67.89"));
		assertTrue(filter.isInvalidAddress("10.240.5.177"));
		assertTrue(filter.isInvalidAddress("195.21.5.65"));
		assertTrue(filter.isInvalidAddress("22.45.67.89"));
	}
}
