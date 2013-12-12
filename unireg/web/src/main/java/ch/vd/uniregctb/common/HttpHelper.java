package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.servlet.ActionExceptionFilter;

public abstract class HttpHelper {

	public static String getRedirectPagePrecedente(HttpServletRequest request) {
		final String referrer = getReferrer(request);
		if (StringUtils.isNotBlank(referrer)) {
			return String.format("redirect:%s", referrer);
		}
		else {
			return "redirect:/404.do";
		}
	}

	public static String getReferrer(HttpServletRequest request) {
		String referrer = (String) request.getSession().getAttribute(ActionExceptionFilter.LAST_GET_URL);
		if (StringUtils.isBlank(referrer)) {
			referrer = request.getHeader("referer"); // Yes, with the legendary misspelling.
		}
		return referrer;
	}

}
