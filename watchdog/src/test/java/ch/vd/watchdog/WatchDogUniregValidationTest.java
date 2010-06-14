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

	public void testRien() {

	}
	
	// Désactivé parce que y en a marre de se faire spammer par la validation qui est down tous les lundis matins
	public void _testValidation() throws Exception {
		LOGGER.info("Vérification de Unireg en validation...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/val-unireg/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	// Désactivé parce que y en a marre de se faire spammer par la validation qui est down tous les lundis matins
	public void _testValidationConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en validation...");
		final HtmlPage page = (HtmlPage) webClient.getPage(new URL(
				"https://validation.portail.etat-de-vaud.ch/fiscalite/val-unireg/admin/status.do"));
		assertNotNull(page);
		assertStatus("OK", page, "serviceCivilStatus");
		assertStatus("OK", page, "serviceInfraStatus");
		assertStatus("OK", page, "serviceSecuriteStatus");
		// SIPF est down jusqu'au 7 juin 2010 : assertStatus("OK", page, "bvrPlusStatus");
	}
}
