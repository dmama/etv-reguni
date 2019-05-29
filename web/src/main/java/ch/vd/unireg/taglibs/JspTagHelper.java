package ch.vd.unireg.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;

public final class JspTagHelper {

	private JspTagHelper() {
	}

	@NotNull
	public static WebApplicationContext getWebApplicationContext(@NotNull PageContext pageContext) {
		final WebApplicationContext context = RequestContextUtils.findWebApplicationContext((HttpServletRequest) pageContext.getRequest());
		if (context == null) {
			throw new IllegalArgumentException("Impossible de trouver le context Spring");
		}
		return context;
	}
}
