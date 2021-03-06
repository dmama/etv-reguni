package ch.vd.unireg.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class WebitTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebitTest.class);
	private static final Pattern valiPattern = Pattern.compile("( *---.{4}-)");

	protected String shortVersion;  // e.g. 19R3
	protected String longVersion;   // e.g. 19R3.A.0-SNAPSHOT

	protected String baseUrl;
	protected String baseWsUrl;
	protected String username;
	protected String password;
	protected String batchUrl;
	protected String securiteUrl;
	protected String tiers1Url;
	protected String tiers2Url;
	protected String party3Url;
	protected String v5Url;
	protected String v6Url;
	protected String v7Url;

	protected WebClient webClient;

	/**
	 * Supprime l'éventuel pattern "---VALI-" ou "---TEST-" ajouté aux DB de validation/test.
	 *
	 * @param string une chaine de caractères.
	 * @return la chaîne de caractères sans le pattern de validation
	 */
	public static String trimValiPattern(String string) {
		if (string == null) {
			return null;
		}
		else {
			return StringUtils.trimToEmpty(valiPattern.matcher(string).replaceAll(""));
		}
	}

	@Before
	public void onSetUp() throws Exception {

		// Charge les propriétés propre aux tests Web-IT
		Properties propsWebIT = new Properties();
		try (InputStream inStream = getClass().getResourceAsStream("/ut/unireg-webit.properties")) {
			propsWebIT.load(inStream);
		}

		// Charge les propriétés générales pour les tests UT
		final String uniregUTPropertiesFile = propsWebIT.getProperty("unireg-ut.properties");
		Properties propsUT = new Properties();
		try (InputStream inStream = new FileInputStream(uniregUTPropertiesFile)) {
			propsUT.load(inStream);
		}

		// Récupère la version d'Unireg
		final String versionFile = propsWebIT.getProperty("version.txt");
		shortVersion = FileUtils.readLines(new File(versionFile), "UTF-8").stream()
				.filter(line -> line.startsWith("short="))
				.map(line -> line.substring(6)) // short=19R3 => 19R3
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Le fichier [" + versionFile + "] ne contient pas la version applicative"));
		longVersion = FileUtils.readLines(new File(versionFile), "UTF-8").stream()
				.filter(line -> line.startsWith("long="))
				.map(line -> line.substring(5)) // long=19R3.A.0-SNAPSHOT => 19R3.A.0-SNAPSHOT
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Le fichier [" + versionFile + "] ne contient pas la version applicative"));

		// Récupère les valeurs des propriétés 
		baseUrl = propsWebIT.getProperty("unireg.baseurl") + propsUT.getProperty("testprop.unireg.deploymenturl");
		baseWsUrl = propsWebIT.getProperty("unireg.baseurl") + propsUT.getProperty("testprop.uniregws.deploymenturl");

		username = propsWebIT.getProperty("webservice.username");
		password = propsWebIT.getProperty("webservice.password");

		batchUrl = baseUrl + propsWebIT.getProperty("webservice.batch.serverurl");
		securiteUrl = baseWsUrl + propsWebIT.getProperty("webservice.securite.serverurl");
		tiers1Url = baseWsUrl + propsWebIT.getProperty("webservice.tiers.serverurl");
		tiers2Url = baseWsUrl + propsWebIT.getProperty("webservice.tiers2.serverurl");
		party3Url = baseWsUrl + propsWebIT.getProperty("webservice.party3.serverurl");
		v5Url = baseWsUrl + propsWebIT.getProperty("webservice.v5.serverurl");
		v6Url = baseWsUrl + propsWebIT.getProperty("webservice.v6.serverurl");
		v7Url = baseWsUrl + propsWebIT.getProperty("webservice.v7.serverurl");

		LOGGER.debug("baseUrl: " + baseUrl);
		LOGGER.debug("baseWsUrl: " + baseWsUrl);

		webClient = new WebClient();
		webClient.getCookieManager().setCookiesEnabled(true);
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
	}

	@After
	public void onTearDown() throws Exception {
	}

	protected HtmlPage getHtmlPage(String relativeUrl) throws Exception {
		LOGGER.debug("Calling HTML page: " + relativeUrl);
		URL url = getAbsoluteUrl(relativeUrl);
		return (HtmlPage) webClient.getPage(url);
	}

	protected URL getAbsoluteUrl(String u) throws Exception {
		return new URL(baseUrl + u);
	}

	/**
	 * Charge un script DB unit dans la base de données de l'application testée
	 *
	 * @param filename le nom du fichier DB unit à charger
	 * @throws Exception en cas de problème
	 */
	public void loadDatabase(String filename) throws Exception {

		LOGGER.info("Chargement du fichier DB unit " + filename);

		// Détermine l'URL de la page d'import
		final URL importPageUrl = new URL(baseUrl + "/admin/tiersImport/list.do");

		// Détermine l'URL du fichier DB unit
		final URL dbUnitUrl = getClass().getResource(filename);
		final String dbUnitPath = dbUnitUrl.getPath().replaceAll("%5C", "/");   // sous Windows, il faut remplacer les backslashes (%5C) par des forwardslashes pour que htmlunit retrouve ses petits...

		// Charge la page d'import
		HtmlPage page = webClient.getPage(importPageUrl);

		// Rempli le champ d'import avec le chemin vers le fichier DB unit
		HtmlFileInput file = page.getHtmlElementById("scriptData");
		file.setValueAttribute(dbUnitPath);

		// Exécute le formulaire
		final HtmlElement element = page.getHtmlElementById("charger");
		HtmlSubmitInput charger = (HtmlSubmitInput) element;
		final Page resultat = charger.click();

		// Vérification que tout s'est bien passé
		final String content = resultat.getWebResponse().getContentAsString();
		assertScriptImported(content);

		// on attend que l'événement de chargement de la base arrive dans le web-service
		Thread.sleep(500);
	}

	private static void assertScriptImported(String content) {
		String error = null;
		if (content == null) {
			error = "la page est vide";
		}
		else if (!content.contains("Les tiers suivants sont présents dans la base de données")) {
			Pattern flashErrorPattern = Pattern.compile(".*<div id=\"flashdisplay\" class=\"flash-error\">(.*?)</div>.*", Pattern.MULTILINE | Pattern.DOTALL);
			final Matcher matcher = flashErrorPattern.matcher(content);
			if (matcher.matches()) {
				error = matcher.group(1);
			}
			else {
				error = "erreur inconnue";
			}
		}
		if (error != null) {
			fail("Le script DB unit ne s'est pas importé correctement : " + error);
		}
	}

	protected void assertNatureTiers(String nature, long tiersId) throws Exception {

		final HtmlPage page = getHtmlPage("/tiers/visu.do?id=" + tiersId);
		assertNotNull(page);

		final List<?> list = page.getElementsByName("debugNatureTiers");
		assertNotNull(list);
		assertEquals(1, list.size());

		final HtmlInput input = (HtmlInput) list.get(0);
		assertNotNull(input);
		assertEquals(nature, input.getValueAttribute());
	}

	protected static void assertEmpty(Collection<?> coll) {
		assertTrue(coll == null || coll.isEmpty());
	}

	protected static void assertEmpty(String message, Collection<?> coll) {
		assertTrue(message, coll == null || coll.isEmpty());
	}

	protected static void assertContains(String containee, String container, String msg) {
		if (container == null || containee == null || !container.contains(containee)) {
			fail(msg);
		}
	}

	protected static void assertContains(String containee, String container) {
		assertContains(containee, container, '\'' + container + "' doesn't contain '" + containee + '\'');
	}

	@SuppressWarnings("unchecked")
	protected static void assertContains(String contenu, HtmlPage page) throws Exception {

		// vérification du contenu
		boolean trouve = false;
		for (HtmlElement element : page.getHtmlElementDescendants()) {
			if (element.asText().contains(contenu)) {
				trouve = true;
				break;
			}
		}
		if (!trouve) {
			fail("Le corps de la page ne contient pas le texte '" + contenu + "'.");
		}
	}
}
