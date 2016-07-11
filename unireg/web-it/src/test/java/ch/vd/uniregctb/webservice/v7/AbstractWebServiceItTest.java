package ch.vd.uniregctb.webservice.v7;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebServiceItTest.class);

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

	protected static class AuthenticationInterceptor implements ClientHttpRequestInterceptor {
		private final String username;
		private final String password;

		public AuthenticationInterceptor(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			final String auth = String.format("%s:%s", username, password);
			final Charset charset = Charset.forName("UTF-8");
			final byte[] base64 = Base64.encodeBase64(auth.getBytes(charset));
			final HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
			wrapper.getHeaders().set("Authorization", String.format("Basic %s", new String(base64, charset)));
			return execution.execute(wrapper, body);
		}
	}

	protected RestTemplate buildTemplate(MediaType... acceptedTypes) {
		final RestTemplate template = new RestTemplate();
		final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		if (acceptedTypes != null && acceptedTypes.length > 0) {
			interceptors.add(new AcceptHeaderHttpRequestInterceptor(acceptedTypes));
		}
		if (StringUtils.isNotBlank(username)) {
			interceptors.add(new AuthenticationInterceptor(username, password));
		}
		template.setInterceptors(interceptors);
		return template;
	}

	protected <T> ResponseEntity<T> get(Class<T> clazz, @NotNull MediaType acceptedMediaType, String uri, Map<String, ?> params) {
		final RestTemplate template = buildTemplate(acceptedMediaType);
		try {
			final ResponseEntity<T> response = template.getForEntity(v7Url + uri, clazz, params);
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
		final RestTemplate template = buildTemplate();
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(dataType);
		try {
			template.put(v7Url + uri, new HttpEntity<>(data, headers), params);
			return HttpStatus.OK;
		}
		catch (HttpStatusCodeException e) {
			final String msg = String.format("%s (%s)", e.getStatusText(), e.getResponseBodyAsString());
			LOGGER.error(msg);
			return e.getStatusCode();
		}
	}
}
