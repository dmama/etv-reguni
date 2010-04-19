package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation,
 * préproduction) sont bien accessibles.
 */
public class WatchDogUniregFormationTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregFormationTest.class);

	public void testFormation() throws Exception {
		LOGGER.info("Vérification de Unireg en formation...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/form-unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}
}