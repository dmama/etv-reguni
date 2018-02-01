package ch.vd.uniregctb.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthenticationHelperTest extends WithoutSpringTest {

	@Test
	public void testPushPopPrincipal() {

		assertFalse(AuthenticationHelper.isAuthenticated());
		assertFalse(AuthenticationHelper.hasCurrentPrincipal());

		AuthenticationHelper.pushPrincipal("test");
		assertFalse(AuthenticationHelper.isAuthenticated());
		assertTrue(AuthenticationHelper.hasCurrentPrincipal());
		assertEquals("test", AuthenticationHelper.getCurrentPrincipal());

		AuthenticationHelper.popPrincipal();
		assertFalse(AuthenticationHelper.isAuthenticated());
		assertFalse(AuthenticationHelper.hasCurrentPrincipal());
	}

	@Test
	public void testPushPopPrincipalWithPreviousAuthentification() {

		assertFalse(AuthenticationHelper.isAuthenticated());
		assertFalse(AuthenticationHelper.hasCurrentPrincipal());

		AuthenticationHelper.pushPrincipal("previous");
		assertFalse(AuthenticationHelper.isAuthenticated());
		assertTrue(AuthenticationHelper.hasCurrentPrincipal());
		assertEquals("previous", AuthenticationHelper.getCurrentPrincipal());

		AuthenticationHelper.pushPrincipal("test");
		assertFalse(AuthenticationHelper.isAuthenticated());
		assertTrue(AuthenticationHelper.hasCurrentPrincipal());
		assertEquals("test", AuthenticationHelper.getCurrentPrincipal());

		AuthenticationHelper.popPrincipal();
		assertFalse(AuthenticationHelper.isAuthenticated());
		assertTrue(AuthenticationHelper.hasCurrentPrincipal());
		assertEquals("previous", AuthenticationHelper.getCurrentPrincipal());

		AuthenticationHelper.resetAuthentication();
		assertFalse(AuthenticationHelper.isAuthenticated());
		assertFalse(AuthenticationHelper.hasCurrentPrincipal());
	}
}
