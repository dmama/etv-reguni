package ch.vd.uniregctb.common;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import junit.framework.Assert;
import org.apache.log4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class WebitTest extends WithoutSpringTest {

	private static final Logger LOGGER = Logger.getLogger(WebitTest.class);
	private static final Pattern valiPattern = Pattern.compile("( *---.{4}-)");

	protected String baseUrl;
	protected String baseWsUrl;
	protected String username;
	protected String password;
	protected String batchUrl;
	protected String securiteUrl;
	protected String tiers1Url;
	protected String tiers2Url;
	protected String tiers3Url;

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
			return valiPattern.matcher(string).replaceAll("").trim();
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		// Charge les propriétés propre aux tests Web-IT
		Properties propsWebIT = new Properties();
		InputStream inStream = getClass().getResourceAsStream("/ut/unireg-webit.properties");
		try {
			propsWebIT.load(inStream);
		}
		finally {
			inStream.close();
		}

		// Charge les propriétés générales pour les tests UT
		final String uniregUTPropertiesFile = propsWebIT.getProperty("unireg-ut.properties");
		Properties propsUT = new Properties();
		inStream = new FileInputStream(uniregUTPropertiesFile);
		try {
			propsUT.load(inStream);
		}
		finally {
			inStream.close();
		}

		// Récupère les valeurs des propriétés 
		baseUrl = propsWebIT.getProperty("unireg.baseurl") + propsUT.getProperty("testprop.unireg.deploymenturl");
		baseWsUrl = propsWebIT.getProperty("unireg.baseurl") + propsUT.getProperty("testprop.uniregws.deploymenturl");

		username = propsWebIT.getProperty("webservice.username");
		password = propsWebIT.getProperty("webservice.password");

		batchUrl = baseUrl + propsWebIT.getProperty("webservice.batch.serverurl");
		securiteUrl = baseWsUrl + propsWebIT.getProperty("webservice.securite.serverurl");
		tiers1Url = baseWsUrl + propsWebIT.getProperty("webservice.tiers.serverurl");
		tiers2Url = baseWsUrl + propsWebIT.getProperty("webservice.tiers2.serverurl");
		tiers3Url = baseWsUrl + propsWebIT.getProperty("webservice.tiers3.serverurl");

		LOGGER.debug("baseUrl: " + baseUrl);
		LOGGER.debug("baseWsUrl: " + baseWsUrl);

		webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
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

		// Charge la page d'import
		HtmlPage page = (HtmlPage) webClient.getPage(importPageUrl);

		// Rempli le champ d'import avec le chemin vers le fichier DB unit
		HtmlFileInput file = (HtmlFileInput) page.getHtmlElementById("scriptData");
		file.setValueAttribute(dbUnitUrl.getPath());

		// Exécute le formulaire
		final HtmlElement element = page.getHtmlElementById("charger");
		HtmlSubmitInput charger = (HtmlSubmitInput) element;
		final Page resultat = charger.click();

		// Vérification que tout s'est bien passé
		final String content = resultat.getWebResponse().getContentAsString();
		assertContains("Les tiers suivants sont présents dans la base de données", content, "Le script DB unit ne s'est pas importé correctement !");
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
			Assert.fail("Le corps de la page ne contient pas le texte '" + contenu + "'.");
		}
	}
}
