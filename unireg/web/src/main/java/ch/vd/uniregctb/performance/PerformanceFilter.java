package ch.vd.uniregctb.performance;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:sebastien.diaz@ciev.vd.ch">S�bastien Diaz</a>
 */
public class PerformanceFilter implements Filter {

	private final static Logger LOGGER = Logger.getLogger(PerformanceFilter.class);

	private static final List<String> COMPILED_JSP = new ArrayList<String>();

	public PerformanceFilter() {
		super();
	}

	private FilterConfig filterConfig = null;

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		LOGGER.debug("init");
		this.filterConfig = filterConfig;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
		LOGGER.debug("destroy");
		this.filterConfig = null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (filterConfig == null)
			return;

		long start = System.nanoTime();
		chain.doFilter(request, response);
		long end = System.nanoTime();
		long duration = (end - start)/1000; // En microsecondes

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String requestURI = httpServletRequest.getRequestURI().replaceAll(httpServletRequest.getContextPath(), "");

		if (!COMPILED_JSP.contains(requestURI)) {
			// this is the first time the URL is requested. As the JSP is
			// not
			// yet compiled, the duration is not relevant.
			COMPILED_JSP.add(requestURI);
		}
		else {
			PerformanceLogsRepository repo = PerformanceLogsRepository.getInstance();
			repo.addLog(PerformanceLogsRepository.CONTROLLER, requestURI, duration);
		}
	}

}
