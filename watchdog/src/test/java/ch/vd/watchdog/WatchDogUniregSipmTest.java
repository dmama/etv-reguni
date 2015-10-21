package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que l'environnement SIPM de l'application Unireg est bien accessible.
 */
public class WatchDogUniregSipmTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregSipmTest.class);

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testSipm() throws Exception {
		LOGGER.info("Vérification de Unireg sur l'environnement SIPM...");

		// pour le moment, on n'a pas encore IAM
		final WebClient webClient = new WebClient();
		final HtmlPage page = getPage(webClient, new URL("http://slv2984v.etat-de-vaud.ch:50600/fiscalite/test-unireg/web/"));
//		final WebClient webClient = loginIamValidation();
//		HtmlPage page = getPage(webClient, new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/test-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre, titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testSipmConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg sur l'environnement SIPM...");
		checkStatus("slv2984v", 50609, false);
	}
}
