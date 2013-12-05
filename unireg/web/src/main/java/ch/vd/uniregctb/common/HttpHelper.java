package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public abstract class HttpHelper {

	public static String getRedirectPagePrecedente(HttpServletRequest request) {
		final String previousPage = request.getHeader("referer");
		if (StringUtils.isNotBlank(previousPage)) {
			return String.format("redirect:%s", previousPage);
		}
		else {
			return "redirect:/404.do";
		}
	}

}
