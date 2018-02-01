package ch.vd.uniregctb.servlet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.uniregctb.common.URLHelper;

/**
 * Ce filtre intercepte toutes les requêtes Http de type GET et qui demandent une page Html et stocke la dernière URL dans la session. <p/> Utilisé en conjonction avec la classe {@link
 * ActionExceptionResolver}, il permet de réafficher la dernière page visitée qui a levé une exception de type {@link ch.vd.registre.base.validation.ValidationException} ou {@link
 * ch.vd.uniregctb.common.ActionException}.
 */
public class ActionExceptionFilter extends GenericFilterBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionExceptionFilter.class);

	public static final String LAST_GET_URL = "last-get-url";

	private static final String KEEP_ATTRIBUTE_NAME = "url_memorize";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		try {
			final HttpServletRequest httpRequest = (HttpServletRequest) request;
			if (isGetForHtml(httpRequest) && shouldMemorizeUrl(httpRequest)) {
				// si la requête utilise la méthode GET et demande de l'Html (= pas de l'ajax ni le résultat de la soumission d'un formulaire), on mémorise l'url
				// (on ne la mémorise pas non plus si le paramètre url_memorize est à faux dans la requête)
				final String url = URLHelper.getTargetUrl(httpRequest);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Last get url = " + url);
				}
				httpRequest.getSession().setAttribute(LAST_GET_URL, url);
			}
		}
		catch (Exception e) {
			// inutile de gérer une exception, au pire on aura pas mis-à-jour la dernière url
		}

		chain.doFilter(request, response);
	}

	/**
	 * @param httpRequest une requête Http
	 * @return <b>vrai</b> si la requête spécifiée utilise la méthode GET et qu'elle demande une page Html en retour.
	 */
	private static boolean isGetForHtml(HttpServletRequest httpRequest) {
		if (httpRequest.getMethod().equals(HttpMethod.GET.name())) {
			final HttpInputMessage inputMessage = new ServletServerHttpRequest(httpRequest);
			final List<MediaType> acceptedMediaTypes = inputMessage.getHeaders().getAccept();
			if (acceptedMediaTypes != null) {
				if (acceptedMediaTypes.contains(MediaType.TEXT_HTML)) {
					return true;
				}
				else if (acceptedMediaTypes.contains(MediaType.ALL)) {
					// [SIFISC-7832] IE8 demande les pages .do avec un "Accept: */*" sans demander le type html spécifiquement.
					// Comme workaround, on considère */* comme du html, pour autant qu'on ne demande pas explicitement du json.
					return !acceptedMediaTypes.contains(MediaType.APPLICATION_JSON);
				}
			}
		}
		return false;
	}

	private static boolean shouldMemorizeUrl(HttpServletRequest httpRequest) {
		final String[] array = httpRequest.getParameterValues(KEEP_ATTRIBUTE_NAME);
		boolean mem = true;
		if (array != null) {
			for (String str : array) {
				mem = Boolean.parseBoolean(str);
				if (!mem) {
					break;
				}
			}
		}
		return mem;
	}
}
