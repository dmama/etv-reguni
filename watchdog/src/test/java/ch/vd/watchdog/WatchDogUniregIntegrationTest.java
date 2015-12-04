package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation, préproduction) sont bien accessibles.
 */
public class WatchDogUniregIntegrationTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregIntegrationTest.class);

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testIntegration() throws Exception {
		LOGGER.info("Vérification de Unireg en intégration...");
		final WebClient webClient = loginIamValidation();
		HtmlPage page = getPage(webClient, new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre, titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testIntegrationConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en intégration...");

		// on ne vérifie plus le service SIPF en IN, ils ne sont pas assez souvent là...
		checkStatus("slv2655v", 50609, false, "serviceBVRPlus");
	}
}
