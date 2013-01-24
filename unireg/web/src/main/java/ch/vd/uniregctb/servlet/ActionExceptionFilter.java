package ch.vd.uniregctb.servlet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

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

	public static final String LAST_GET_URL = "last-get-url";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		try {
			final HttpServletRequest httpRequest = (HttpServletRequest) request;
			if (isGetForHtml(httpRequest)) {
				// si la requête utilise la méthode GET et demande de l'Html (= pas de l'ajax ni le résultat de la soumission d'un formulaire), on mémorise l'url
				final String url = URLHelper.getTargetUrl(httpRequest);
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
			if (acceptedMediaTypes != null && acceptedMediaTypes.contains(MediaType.TEXT_HTML)) {
				return true;
			}
		}
		return false;
	}
}
