package ch.vd.watchdog;

import java.net.URL;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

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
}
