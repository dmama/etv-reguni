package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;

/**
 * Teste que les l'environnement intégration de post-production de l'application Unireg est bien accessible.
 */
public class WatchDogUniregIntegarationPostProductionTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregIntegarationPostProductionTest.class);

	public void testIntegrationPostProduction() throws Exception {
		LOGGER.info("Vérification de Unireg en intégration de post-production...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/dev-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	public void testIntegrationPostProductionConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en intégration de post-production...");
		final HtmlPage page = (HtmlPage) webClient.getPage(new URL(
				"https://validation.portail.etat-de-vaud.ch/fiscalite/dev-unireg/web/admin/status.do"));
		assertNotNull(page);
		assertStatus("OK", page, "serviceCivilStatus");
		assertStatus("OK", page, "serviceInfraStatus");
		assertStatus("OK", page, "serviceSecuriteStatus");
		// SIPF, SIPF, SIIIIIIIIIIIIIIIIIIIIPF !!! assertStatus("OK", page, "bvrPlusStatus");
	}
}
