package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que les l'environnement intégration de post-production de l'application Unireg est bien accessible.
 */
public class WatchDogUniregIntegrationPostProductionTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregIntegrationPostProductionTest.class);

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testIntegrationPostProduction() throws Exception {
		LOGGER.info("Vérification de Unireg en intégration de post-production...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/dev-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testIntegrationPostProductionConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en intégration de post-production...");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/dev-unireg/web/admin/status/civil.do");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/dev-unireg/web/admin/status/infra.do");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/dev-unireg/web/admin/status/securite.do");
		// SIPF, SIPF, SIIIIIIIIIIIIIIIIIIIIPF !!! assertStatus("OK", page, "bvrPlusStatus");
	}
}
