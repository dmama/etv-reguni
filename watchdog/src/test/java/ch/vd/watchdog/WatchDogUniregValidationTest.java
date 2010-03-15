package ch.vd.watchdog;

import java.net.URL;

import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation,
 * préproduction) sont bien accessibles.
 */
public class WatchDogUniregValidationTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregValidationTest.class);

	public void testValidationPP() throws Exception {
		LOGGER.info("Vérification de Unireg PP en validation...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/val-unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	public void testValidationIS() throws Exception {
		LOGGER.info("Vérification de Unireg IS en validation...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/form-unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}
}
