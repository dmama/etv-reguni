package ch.vd.watchdog;

import java.net.URL;

import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation,
 * préproduction) sont bien accessibles.
 */
public class WatchDogUniregIntegarationISTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregIntegarationISTest.class);

	// ignoré parce que plus actif
	public void _testIntegrationPP() throws Exception {
		LOGGER.info("Vérification de Unireg PP en intégration...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	public void testIntegrationIS() throws Exception {
		LOGGER.info("Vérification de Unireg IS en intégration...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg-is/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	public void testIntegrationISConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg IS en intégration...");
		final HtmlPage page = (HtmlPage) webClient.getPage(new URL(
				"https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg-is/admin/status.do"));
		assertNotNull(page);
		assertStatus("OK", page, "serviceCivilStatus");
		assertStatus("OK", page, "serviceInfraStatus");
		assertStatus("OK", page, "serviceSecuriteStatus");
	}

	private static void assertStatus(final String expected, final HtmlPage page, final String statusIdName) {
		final HtmlTableCell td = (HtmlTableCell) page.getHtmlElementById(statusIdName);
		assertNotNull(td);
		final DomText status = (DomText) td.getFirstDomChild();
		assertNotNull(status);
		assertEquals("Problème avec le " + statusIdName, expected, status.asText());
	}
}
