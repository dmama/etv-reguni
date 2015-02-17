package ch.vd.watchdog;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation, préproduction) sont bien accessibles.
 */
public abstract class WatchDogTest {

	public static final long TIMEOUT = 30000;

	private static final Logger LOGGER = Logger.getLogger(WatchDogTest.class);

	private static final String IAM_USERNAME = "usrreg06";
	private static final String IAM_PASSWORD = "Welc0me!"; // please don't look

	private MyWebClient webClient;

	protected static final class MyWebClient extends WebClient {

		private boolean preventExceptionThrowingOnWrongStatusCode = false;

		@Override
		public Page loadWebResponseInto(WebResponse webResponse, WebWindow webWindow) throws IOException, FailingHttpStatusCodeException {
			if (webResponse.getStatusCode() / 100 == 3) {
				// redirect : 3xx
				final String location = webResponse.getResponseHeaderValue("Location");
				final URL url = resolveURL(webResponse.getWebRequest().getUrl(), location);
				final WebRequest request = new WebRequest(url, getBrowserVersion().getHtmlAcceptHeader());
				return loadWebResponseInto(loadWebResponse(request), webWindow);
			}

			return super.loadWebResponseInto(webResponse, webWindow);
		}

		@Override
		public void throwFailingHttpStatusCodeExceptionIfNecessary(WebResponse webResponse) {
			if (!preventExceptionThrowingOnWrongStatusCode) {
				super.throwFailingHttpStatusCodeExceptionIfNecessary(webResponse);
			}
		}
	}

	@Before
	public void setUp() throws Exception {
		webClient = new MyWebClient();

		final WebClientOptions options = webClient.getOptions();
		options.setUseInsecureSSL(true);
		options.setRedirectEnabled(false);

		options.setJavaScriptEnabled(true);
		loginIAM();
		options.setJavaScriptEnabled(false);
	}

	private static final Pattern COOKIE_PATTERN = Pattern.compile("([^;]*)(?:; expires=([^;]*))?(?:; domain=([^;]*))?(?:; path=([^;]*))?(; secure)?");

	private static final Pattern COOKIE_NAME_VALUE_PATTERN = Pattern.compile("(.*)=(.*)");
	private static final Pattern PORT_EXTRACTOR = Pattern.compile("(.*):(\\d+)");
	private static final Map<String, Integer> DEFAULT_PORTS = buildDefaultPorts();

	private static Map<String, Integer> buildDefaultPorts() {
		final Map<String, Integer> map = new HashMap<>();
		map.put("http", 80);
		map.put("https", 443);
		return map;
	}

	/**
	 * Explicit following of redirect
	 * @param url url to fetch
	 * @return the HTML page found at the end of all the redirects
	 */
	public <P extends Page> P getPage(URL url) throws IOException {
		return getPage(url, 20);
	}

	/**
	 * Explicit following of redirect
	 * @param url url to fetch
	 * @param nbAllowedRedirects number of allowed redirects
	 * @return the HTML page found at the end of all the redirects
	 */
	private <P extends Page> P getPage(URL url, int nbAllowedRedirects) throws IOException {
		try {
			return webClient.getPage(url);
		}
		catch (FailingHttpStatusCodeException e) {
			// redirect 3xx
			if (e.getStatusCode() / 100 == 3) {
				if (nbAllowedRedirects == 0) {
					throw new IOException("Too many redirects", e);
				}
				final WebResponse response = e.getResponse();
				final CookieManager cm = webClient.getCookieManager();
				final List<NameValuePair> headers = response.getResponseHeaders();
				for (NameValuePair nvp : headers) {
					if ("Set-Cookie".equalsIgnoreCase(nvp.getName())) {
						final Matcher matcher = COOKIE_PATTERN.matcher(nvp.getValue());
						if (matcher.matches()) {
							final String cookieNameValue = matcher.group(1);
							final Matcher nvMatcher = COOKIE_NAME_VALUE_PATTERN.matcher(cookieNameValue);
							final String cookieName;
							final String cookieValue;
							if (nvMatcher.matches()) {
								cookieName = nvMatcher.group(1);
								cookieValue = nvMatcher.group(2);
							}
							else {
								cookieName = cookieNameValue;
								cookieValue = null;
							}

							final String cookieExplicitDomain = StringUtils.trimToNull(matcher.group(3));
							final String cookiePath = StringUtils.trimToNull(matcher.group(4));
							cm.addCookie(new Cookie(cookieExplicitDomain != null ? cookieExplicitDomain : url.getHost(), cookieName, cookieValue, cookiePath, null, true));
						}
					}
				}

				final String location = response.getResponseHeaderValue("Location");
				final URL newUrl = resolveURL(url, location);
				return getPage(newUrl, nbAllowedRedirects - 1);
			}
			throw e;
		}
	}

	private static URL resolveURL(URL source, String relative) throws MalformedURLException {
		final URL newUrl = WebClient.expandUrl(source, relative);
		final String authority = newUrl.getAuthority();
		final Matcher portMatcher = PORT_EXTRACTOR.matcher(authority);
		if (portMatcher.matches()) {
			final int explicitPort = Integer.parseInt(portMatcher.group(2));
			final String protocol = newUrl.getProtocol();

			final Integer defaultPort = DEFAULT_PORTS.get(protocol);
			if (defaultPort != null && explicitPort == defaultPort) {
				return new URL(protocol, portMatcher.group(1), -1, newUrl.getFile());
			}
		}
		return newUrl;
	}

	/**
	 * Log l'utilisateur dans IAM, si nécessaire.
	 */
	private void loginIAM() throws Exception {

		final HtmlPage loginPage = getPage(new URL("https://validation.portail.etat-de-vaud.ch/iam/accueil/"));
		if (loginPage.getTitleText().contains("Page de Login")) {
			LOGGER.debug("Login IAM avec username = " + IAM_USERNAME);

			final HtmlTextInput utilisateur = loginPage.getHtmlElementById("IDToken1");
			final HtmlPasswordInput password = loginPage.getHtmlElementById("IDToken2");

			utilisateur.setValueAttribute(IAM_USERNAME);
			password.setValueAttribute(IAM_PASSWORD);

			webClient.preventExceptionThrowingOnWrongStatusCode = true;
			final ScriptResult resultat = loginPage.executeJavaScript("javascript:LoginSubmit('Log In')");
			final HtmlPage page = (HtmlPage) resultat.getNewPage();
			assertNotNull(page);
			webClient.preventExceptionThrowingOnWrongStatusCode = false;
		}
	}

	protected static void assertStatus(final String expected, final HtmlPage page, final String statusIdName) {
		final HtmlTableCell td = page.getHtmlElementById(statusIdName);
		assertNotNull(td);
		final DomText status = (DomText) td.getFirstChild();
		assertNotNull(status);
		assertEquals("Problème avec le " + statusIdName, expected, status.asText());
	}

	/**
	 * Asserte que le statut du service est bien la valeur spécifiée. Cette méthode fonctionne à partir de la version 5.0.1 d'Unireg (qui permet de récupérer les statuts des différents services au format
	 * JSON).
	 *
	 * @param expected le statut attendu (en général "OK")
	 * @param url      l'url d'accès au statut du service sous format JSON
	 * @throws IOException en cas d'impossibilité de récupérer le statut
	 */
	protected void assertJsonStatus(String expected, String url) throws IOException {

		final Page page = getPage(new URL(url));
		assertNotNull(page);

		final WebResponse response = page.getWebResponse();
		assertEquals("application/json", response.getContentType());

		final String json = response.getContentAsString();
		final Map<String, String> map = new Gson().fromJson(json, new TypeToken<Map<String, String>>() {
		}.getType());

		final String actual = map.get("code");
		assertNotNull(actual);

		if (!expected.equals(actual)) {
			fail("Problème avec le " + map.get("name") + ". Description = " + map.get("description"));
		}
	}
}
