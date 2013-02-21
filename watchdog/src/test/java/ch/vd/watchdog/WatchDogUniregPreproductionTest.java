package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation, préproduction) sont bien accessibles.
 */
public class WatchDogUniregPreproductionTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregPreproductionTest.class);

	// @Test(timeout = WatchDogTest.TIMEOUT) Désactivé temporairement pendant la mise-en-préproduction
	public void testPreproduction() throws Exception {
		LOGGER.info("Vérification de Unireg PP en préproduction...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Choisissez votre OID de travail"));
	}
}
