package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation, préproduction) sont bien accessibles.
 */
public class WatchDogUniregFormationTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregFormationTest.class);

	// @Test(timeout = WatchDogTest.TIMEOUT) Désactivé parce que  - apparemment - la formation n'est plus utilisée pour l'instant
	public void testFormation() throws Exception {
		LOGGER.info("Vérification de Unireg en formation...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/form-unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	// @Test(timeout = WatchDogTest.TIMEOUT) Désactivé parce que  - apparemment - la formation n'est plus utilisée pour l'instant
	public void testFormationConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en formation...");
		final HtmlPage page = (HtmlPage) webClient.getPage(new URL(
				"https://validation.portail.etat-de-vaud.ch/fiscalite/form-unireg/admin/status.do"));
		assertNotNull(page);
		assertStatus("OK", page, "serviceCivilStatus");
		assertStatus("OK", page, "serviceInfraStatus");
		assertStatus("OK", page, "serviceSecuriteStatus");
		assertStatus("OK", page, "bvrPlusStatus");
	}
}
