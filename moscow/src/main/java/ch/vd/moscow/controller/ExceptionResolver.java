package ch.vd.moscow.controller;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

public class ExceptionResolver extends SimpleMappingExceptionResolver {

	private static final Logger LOGGER = Logger.getLogger(ExceptionResolver.class);

	@Override
	protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		LOGGER.warn("Exception thrown when handling request " + getMethod(request) + "[" + getUrl(request) + "]", ex);
		return super.doResolveException(request, response, handler, ex);
	}

	private static String getMethod(ServletRequest servletRequest) {
		if (servletRequest instanceof HttpServletRequest) {
			final HttpServletRequest req = (HttpServletRequest) servletRequest;
			return req.getMethod();
		}
		return "n/a";
	}

	private static String getUrl(ServletRequest servletRequest) {
		if (servletRequest instanceof HttpServletRequest) {
			return URLHelper.getTargetUrl((HttpServletRequest) servletRequest);
		}
		else {
			return "n/a";
		}
	}
}
