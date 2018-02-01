package ch.vd.unireg.common;

public final class AuthenticationInterface implements ch.vd.shared.batchtemplate.AuthenticationInterface {

	public static final AuthenticationInterface INSTANCE = new AuthenticationInterface();

	@Override
	public String getCurrentPrincipal() {
		return AuthenticationHelper.getCurrentPrincipal();
	}

	@Override
	public void pushPrincipal(String principal) {
		AuthenticationHelper.pushPrincipal(principal);
	}

	@Override
	public void popPrincipal() {
		AuthenticationHelper.popPrincipal();
	}
}
