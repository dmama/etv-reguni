package ch.vd.uniregctb.webservice.v5;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import ch.vd.uniregctb.common.WebitTest;

public abstract class AbstractWebServiceItTest extends WebitTest {

	private static final Logger LOGGER = Logger.getLogger(AbstractWebServiceItTest.class);

	/**
	 * Un intercepteur qui permet de spécifier des media-type acceptés
	 */
	protected static class AcceptHeaderHttpRequestInterceptor implements ClientHttpRequestInterceptor {

		private final List<MediaType> acceptHeaderValues;

		public AcceptHeaderHttpRequestInterceptor(MediaType... acceptHeaderValues) {
			if (acceptHeaderValues == null || acceptHeaderValues.length == 0) {
				throw new IllegalArgumentException("acceptHeaderValues");
			}
			this.acceptHeaderValues = Arrays.asList(acceptHeaderValues);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			final HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
			wrapper.getHeaders().setAccept(acceptHeaderValues);
			return execution.execute(wrapper, body);
		}
	}

	protected static RestTemplate buildTemplateWithAcceptHeader(MediaType... mediaTypes) {
		final RestTemplate template = new RestTemplate();
		if (mediaTypes != null && mediaTypes.length > 0) {
			final ClientHttpRequestInterceptor interceptor = new AcceptHeaderHttpRequestInterceptor(mediaTypes);
			template.setInterceptors(Collections.singletonList(interceptor));
		}
		return template;
	}

	protected <T> ResponseEntity<T> get(Class<T> clazz, @NotNull MediaType acceptedMediaType, String uri, Map<String, ?> params) {
		final RestTemplate template = buildTemplateWithAcceptHeader(acceptedMediaType);
		try {
			final ResponseEntity<T> response = template.getForEntity(v5Url + uri, clazz, params);
			Assert.assertNotNull(response);
			return response;
		}
		catch (HttpStatusCodeException e) {
			final String msg = String.format("%s (%s)", e.getStatusText(), e.getResponseBodyAsString());
			LOGGER.error(msg);
			return new ResponseEntity<>(e.getStatusCode());
		}
	}

	protected HttpStatus put(String uri, Map<String, ?> params, Object data, @NotNull MediaType dataType) {
		final RestTemplate template = new RestTemplate();
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(dataType);
		try {
			template.put(v5Url + uri, new HttpEntity<>(data, headers), params);
			return HttpStatus.OK;
		}
		catch (HttpStatusCodeException e) {
			final String msg = String.format("%s (%s)", e.getStatusText(), e.getResponseBodyAsString());
			LOGGER.error(msg);
			return e.getStatusCode();
		}
	}
}
