package ch.vd.watchdog;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation, préproduction) sont bien accessibles.
 */
public abstract class WatchDogTest {

	public static final long TIMEOUT = 30000;

	private static final Logger LOGGER = Logger.getLogger(WatchDogTest.class);

	private static final String IAM_USERNAME = "usrreg06";
	private static final String IAM_PASSWORD = "Welc0me!"; // please don't look

	private static final String JMX_USERNAME = "monitorRole";
	private static final String JMX_PASSWORD = "m0n1t0r";   // please don't look...

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

	protected static WebClient loginIamValidation() throws Exception {
		final MyWebClient webClient = new MyWebClient();

		final WebClientOptions options = webClient.getOptions();
		options.setUseInsecureSSL(true);
		options.setRedirectEnabled(false);

		options.setJavaScriptEnabled(true);
		loginIAM(webClient);
		options.setJavaScriptEnabled(false);
		return webClient;
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
	 * @param webClient http client to use
	 * @param url url to fetch
	 * @return the HTML page found at the end of all the redirects
	 */
	public static <P extends Page> P getPage(WebClient webClient, URL url) throws IOException {
		return getPage(webClient, url, 20);
	}

	/**
	 * Explicit following of redirect
	 * @param url url to fetch
	 * @param nbAllowedRedirects number of allowed redirects
	 * @return the HTML page found at the end of all the redirects
	 */
	private static <P extends Page> P getPage(WebClient webClient, URL url, int nbAllowedRedirects) throws IOException {
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
				return getPage(webClient, newUrl, nbAllowedRedirects - 1);
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
	private static void loginIAM(MyWebClient webClient) throws Exception {

		final HtmlPage loginPage = getPage(webClient, new URL("https://validation.portail.etat-de-vaud.ch/iam/accueil/"));
		if (loginPage.getTitleText().contains("Page de Login")) {
			LOGGER.debug("Login IAM avec username = " + IAM_USERNAME);

			final HtmlTextInput utilisateur = loginPage.getHtmlElementById("txt_callbackItem_NameCallback_IDToken1");
			final HtmlPasswordInput password = loginPage.getHtmlElementById("txt_callbackItem_PasswordCallback_IDToken2");

			utilisateur.setValueAttribute(IAM_USERNAME);
			password.setValueAttribute(IAM_PASSWORD);

			webClient.preventExceptionThrowingOnWrongStatusCode = true;
			final ScriptResult resultat = loginPage.executeJavaScript("javascript:LoginSubmit('Connexion')");
			final HtmlPage page = (HtmlPage) resultat.getNewPage();
			assertNotNull(page);
			webClient.preventExceptionThrowingOnWrongStatusCode = false;
		}
	}

	/**
	 * Asserte que le statut du service est bien la valeur spécifiée. Cette méthode fonctionne à partir de la version 5.0.1 d'Unireg (qui permet de récupérer les statuts des différents services au format
	 * JSON).
	 *
	 * @param expected le statut attendu (en général "OK")
	 * @param url      l'url d'accès au statut du service sous format JSON
	 * @throws IOException en cas d'impossibilité de récupérer le statut
	 */
	protected static void assertJsonStatus(WebClient webClient, String expected, String url) throws IOException {

		final Page page = getPage(webClient, new URL(url));
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

	protected interface JmxCallback<T> {
		T execute(MBeanServerConnection con) throws Exception;
	}

	protected static abstract class JmxCallbackWithoutResult implements JmxCallback<Object> {
		@Override
		public Object execute(MBeanServerConnection con) throws Exception {
			doExecute(con);
			return null;
		}

		protected abstract void doExecute(MBeanServerConnection con) throws Exception;
	}

	protected static <T> T doWithJmxConnection(String host, int port, boolean withCredentials, JmxCallback<T> callback) throws Exception {
		final String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port);
		final JMXServiceURL serviceUrl = new JMXServiceURL(url);
		final Map<String, String[]> env = new HashMap<>();
		if (withCredentials) {
			final String[] credentials = { JMX_USERNAME, JMX_PASSWORD };
			env.put(JMXConnector.CREDENTIALS, credentials);
		}
		try (JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env)) {
			final MBeanServerConnection con = jmxConnector.getMBeanServerConnection();
			return callback.execute(con);
		}
	}

	protected static void checkStatus(String host, int port, boolean withCredentials, String... ignored) throws Exception {
		final Set<String> ignoredSet = new HashSet<>(Arrays.asList(ignored));
		doWithJmxConnection(host, port, withCredentials, new JmxCallbackWithoutResult() {
			@Override
			protected void doExecute(MBeanServerConnection con) throws Exception {
				final Set<ObjectName> beanSet = con.queryNames(new ObjectName("ch.vd.uniregctb-*:type=Monitoring,name=Application"), null);
				final Set<String> nok = new TreeSet<>();
				for (ObjectName objectName : beanSet) {
					final String json = (String) con.getAttribute(objectName, "StatusJSON");
					final String[] splits = json.split("\\s*,\\s*");
					assertTrue(json, splits.length > 0);

					final Pattern pattern = Pattern.compile("[^\\w]*['\"](\\w+)['\"]\\s*:\\s*['\"](\\w+)['\"].*");
					for (String split : splits) {
						final Matcher matcher = pattern.matcher(split);
						if (matcher.matches()) {
							if (!ignoredSet.contains(matcher.group(1))) {
								final String localStatus = matcher.group(2);
								if (!"OK".equals(localStatus)) {
									nok.add(String.format("%s/%s (%s)", objectName, matcher.group(1), localStatus));
								}
							}
						}
						else {
							throw new IllegalArgumentException("Unsupported format : " + split);
						}
					}
				}

				assertEquals(Arrays.toString(nok.toArray(new String[nok.size()])), 0, nok.size());
				assertNotEquals(0, beanSet.size());
			}
		});
	}
}
