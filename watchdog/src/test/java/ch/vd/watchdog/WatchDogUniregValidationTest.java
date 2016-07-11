package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation, préproduction) sont bien accessibles.
 */
public class WatchDogUniregValidationTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregValidationTest.class);

	@Ignore("Tant que l'on n'aura pas résolu l'identification dans le nouvel IAM")
	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testValidation() throws Exception {
		LOGGER.info("Vérification de Unireg en validation...");
		final WebClient webClient = loginIamValidation();
		HtmlPage page = getPage(webClient, new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/val-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre, titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testValidationConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en validation...");
		checkStatus("slv2743v", 30609, true);
	}
}
