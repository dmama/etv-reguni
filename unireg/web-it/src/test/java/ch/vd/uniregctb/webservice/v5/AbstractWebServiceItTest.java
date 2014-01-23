package ch.vd.uniregctb.webservice.v5;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;

import ch.vd.uniregctb.common.WebitTest;

public abstract class AbstractWebServiceItTest extends WebitTest {

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
}
