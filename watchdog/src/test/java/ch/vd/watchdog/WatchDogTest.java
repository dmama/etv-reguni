package ch.vd.watchdog;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation,
 * préproduction) sont bien accessibles.
 */
public abstract class WatchDogTest extends TestCase {

	private static final Logger LOGGER = Logger.getLogger(WatchDogTest.class);

	private static final String IAM_USERNAME = "usrfis06";
	private static final String IAM_PASSWORD = "welc0me"; // please don't look

	protected WebClient webClient;

	@Override
	protected void setUp() throws Exception {
		webClient = new WebClient();
		webClient.setUseInsecureSSL(true);
		super.setUp();

		webClient.setJavaScriptEnabled(true);
		loginIAM();
		webClient.setJavaScriptEnabled(false);
	}

	/**
	 * Log l'utilisateur dans IAM, si nécessaire.
	 */
	private void loginIAM() throws Exception {

		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/iam/accueil/"));
		if (page.getTitleText().contains("Page de Login")) {
			LOGGER.debug("Login IAM avec username = " + IAM_USERNAME);

			HtmlTextInput utilisateur = (HtmlTextInput) page.getHtmlElementById("IDToken1");
			HtmlPasswordInput password = (HtmlPasswordInput) page.getHtmlElementById("IDToken2");

			utilisateur.setValueAttribute(IAM_USERNAME);
			password.setValueAttribute(IAM_PASSWORD);

			ScriptResult resultat = page.executeJavaScript("javascript:LoginSubmit('Log In')");
			page = (HtmlPage) resultat.getNewPage();
			assertNotNull(page);
		}
	}

	protected static void assertStatus(final String expected, final HtmlPage page, final String statusIdName) {
		final HtmlTableCell td = (HtmlTableCell) page.getHtmlElementById(statusIdName);
		assertNotNull(td);
		final DomText status = (DomText) td.getFirstDomChild();
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

		final Page page = webClient.getPage(new URL(url));
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
