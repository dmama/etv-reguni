package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que les différents déploiements de l'application Unireg dans les différents environnements (intégration, validation, formation, préproduction) sont bien accessibles.
 */
public class WatchDogUniregIntegrationTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregIntegrationTest.class);

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testIntegration() throws Exception {
		LOGGER.info("Vérification de Unireg en intégration...");
		HtmlPage page = getPage(new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre, titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testIntegrationConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en intégration...");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/civil.do");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/infra.do");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/securite.do");
		// inutile de se faire spammer pour SIPF assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/bvr.do");
		assertJsonStatus("OK", "https://validation.portail.etat-de-vaud.ch/fiscalite/int-unireg/web/admin/status/efacture.do");
	}
}
