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
public class WatchDogUniregIntegarationTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregIntegarationTest.class);

	public void testIntegration() throws Exception {
		LOGGER.info("Vérification de Unireg en intégration...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	public void testIntegrationConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en intégration...");
		final HtmlPage page = (HtmlPage) webClient.getPage(new URL(
				"https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/admin/status.do"));
		assertNotNull(page);
		assertStatus("OK", page, "serviceCivilStatus");
		assertStatus("OK", page, "serviceInfraStatus");
		assertStatus("OK", page, "serviceSecuriteStatus");
		assertStatus("OK", page, "bvrPlusStatus");
	}

	private static void assertStatus(final String expected, final HtmlPage page, final String statusIdName) {
		final HtmlTableCell td = (HtmlTableCell) page.getHtmlElementById(statusIdName);
		assertNotNull(td);
		final DomText status = (DomText) td.getFirstDomChild();
		assertNotNull(status);
		assertEquals("Problème avec le " + statusIdName, expected, status.asText());
	}
}
