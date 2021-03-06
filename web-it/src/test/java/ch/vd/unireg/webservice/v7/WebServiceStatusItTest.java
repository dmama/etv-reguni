package ch.vd.unireg.webservice.v7;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

public class WebServiceStatusItTest extends AbstractWebServiceItTest {

	@Test
	public void testPing() throws Exception {
		final RestTemplate template = buildTemplate();
		final String response = template.getForObject(v7Url + "/status/ping", String.class);
		Assert.assertNotNull(response);
		Assert.assertTrue(response, Pattern.matches("[0-9]+", response));
	}
}
