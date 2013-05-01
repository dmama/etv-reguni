package ch.vd.uniregctb.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AuthenticationHelperTest extends WithoutSpringTest {

	@Test
	public void testPushPopPrincipal() {

		AuthenticationHelper.pushPrincipal("test");
		assertEquals("test", AuthenticationHelper.getCurrentPrincipal());

		AuthenticationHelper.popPrincipal();
		assertFalse(AuthenticationHelper.isAuthenticated());
	}

	@Test
	public void testPushPopPrincipalWithPreviousAuthentification() {

		AuthenticationHelper.pushPrincipal("previous");
		assertEquals("previous", AuthenticationHelper.getCurrentPrincipal());

		AuthenticationHelper.pushPrincipal("test");
		assertEquals("test", AuthenticationHelper.getCurrentPrincipal());

		AuthenticationHelper.popPrincipal();
		assertEquals("previous", AuthenticationHelper.getCurrentPrincipal());

		AuthenticationHelper.resetAuthentication();
		assertFalse(AuthenticationHelper.isAuthenticated());
	}
}
