package ch.vd.unireg.servlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Ce filtre permet d'ajouter des entêtes sur la réponse HTTP. Par exemple, il permet de renseigner la date d'expiration sur les éléments filtrés (copié-collé de l'implémentation shared-web).
 * <p/>
 * Exemple d'utilisation:
 * <pre>
 *     &lt;filter&gt;
 * 		&lt;filter-name&gt;CachingFilter&lt;/filter-name&gt;
 * 		&lt;filter-class&gt;ch.vd.registre.web.filter.CachingFilter&lt;/filter-class&gt;
 * 		&lt;init-param&gt;
 * 			&lt;param-name&gt;Cache-Control&lt;/param-name&gt;
 * 			&lt;param-value&gt;max-age=2678400&lt;/param-value&gt;&lt;!-- a month, in seconds --&gt;
 * 		&lt;/init-param&gt;
 * 	&lt;/filter&gt;
 * 	&lt;filter-mapping&gt;
 * 		&lt;filter-name&gt;CachingFilter&lt;/filter-name&gt;
 * 		&lt;url-pattern&gt;*.png&lt;/url-pattern&gt;
 * 	&lt;/filter-mapping&gt;
 * 	&lt;filter-mapping&gt;
 * 		&lt;filter-name&gt;CachingFilter&lt;/filter-name&gt;
 * 		&lt;url-pattern&gt;*.jpg&lt;/url-pattern&gt;
 * 	&lt;/filter-mapping&gt;
 * </pre>
 * See http://www.kuligowski.pl/java/rest-style-urls-and-url-mapping-for-static-content-apache-tomcat,5
 */
public class AddResponseHeadersFilter implements Filter {

	private FilterConfig fc;

	@Override
	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletResponse response = (HttpServletResponse) res;

		// set the provided HTTP response parameters
		for (Enumeration<String> e = fc.getInitParameterNames(); e.hasMoreElements();) {
			String headerName = e.nextElement();
			response.addHeader(headerName, fc.getInitParameter(headerName));
		}
		// pass the request/response on
		chain.doFilter(req, response);
	}

	@Override
	public void init(FilterConfig filterConfig) {
		this.fc = filterConfig;
	}

	@Override
	public void destroy() {
		this.fc = null;
	}
}