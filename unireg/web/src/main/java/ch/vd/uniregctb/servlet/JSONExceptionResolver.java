package ch.vd.uniregctb.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

/**
 * Ce resolver va détecter si l'appelant s'attend à recevoir une réponse JSON, et si c'est le cas va convertir l'exception en une réponse JSON.
 */
public class JSONExceptionResolver implements HandlerExceptionResolver, Ordered {

	protected final Logger LOGGER = Logger.getLogger(JSONExceptionResolver.class);

	private int order = Ordered.LOWEST_PRECEDENCE;
	private MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();

	@SuppressWarnings("UnusedDeclaration")
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		try {
			final HttpInputMessage inputMessage = new ServletServerHttpRequest(request);
			final List<MediaType> acceptedMediaTypes = inputMessage.getHeaders().getAccept();
			if (acceptedMediaTypes != null && acceptedMediaTypes.contains(MediaType.APPLICATION_JSON)) {
				// il y a une exception -> error 500
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				// le client attend du JSON, on lui donne du JSON
				final HttpOutputMessage outputMessage = new ServletServerHttpResponse(response);
				jsonConverter.write(ex.getMessage(), MediaType.APPLICATION_JSON, outputMessage);
				return new ModelAndView();
			}
		}
		catch (Exception e) {
			LOGGER.error("Impossible de résoudre l'exception en JSON", e);
		}
		return null;
	}
}
