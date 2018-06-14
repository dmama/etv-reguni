package ch.vd.unireg.common;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.servlet.ActionExceptionFilter;

public abstract class HttpHelper {

	public static String getRedirectPagePrecedenteOuDefaut(HttpServletRequest request, @NotNull Supplier<String> defaultUrlSupplier) {
		final String referrer = getReferrer(request);
		return "redirect:" + (StringUtils.isBlank(referrer) ? defaultUrlSupplier.get() : referrer);
	}

	public static String getRedirectPagePrecedenteOuDefaut(HttpServletRequest request, String defaultUrl) {
		return getRedirectPagePrecedenteOuDefaut(request, () -> defaultUrl);
	}

	public static String getRedirectPagePrecedente(HttpServletRequest request) {
		return getRedirectPagePrecedenteOuDefaut(request, "/errors/404.do");
	}

	public static String getReferrer(HttpServletRequest request) {
		String referrer = (String) request.getSession().getAttribute(ActionExceptionFilter.LAST_GET_URL);
		if (StringUtils.isBlank(referrer)) {
			referrer = request.getHeader("referer"); // Yes, with the legendary misspelling.
		}
		return referrer;
	}

}
