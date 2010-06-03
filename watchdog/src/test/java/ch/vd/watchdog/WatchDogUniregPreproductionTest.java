package ch.vd.watchdog;

import java.net.URL;

import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation,
 * préproduction) sont bien accessibles.
 */
public class WatchDogUniregPreproductionTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregPreproductionTest.class);

	// Désactivé temporairement pendant la mise-en-préproduction
	public void _testPreproduction() throws Exception {
		LOGGER.info("Vérification de Unireg PP en préproduction...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Choisissez votre OID de travail"));
	}
}
