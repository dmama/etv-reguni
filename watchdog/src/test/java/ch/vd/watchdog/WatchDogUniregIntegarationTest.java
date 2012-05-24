package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation,
 * préproduction) sont bien accessibles.
 */
public class WatchDogUniregIntegarationTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregIntegarationTest.class);

	public void testIntegration() throws Exception {
		LOGGER.info("Vérification de Unireg en intégration...");
		HtmlPage page = (HtmlPage) webClient.getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	public void testIntegrationConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en intégration...");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/civil.do");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/hostInfra.do");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/fidor.do");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/securite.do");
		// inutile de se faire spammer pour SIPF assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/bvr.do");
	}
}
