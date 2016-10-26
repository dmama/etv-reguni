package ch.vd.watchdog;

import java.net.URL;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste que l'environnement SIPM de l'application Unireg est bien accessible.
 */
public class WatchDogUniregSipmTest extends WatchDogTest {

	private static final Logger LOGGER = Logger.getLogger(WatchDogUniregSipmTest.class);

	@Ignore("Tant que l'on n'aura pas résolu l'identification dans le nouvel IAM")
	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testSipm() throws Exception {
		LOGGER.info("Vérification de Unireg sur l'environnement SIPM...");

		// pour le moment, on n'a pas encore IAM
		final WebClient webClient = loginIamValidation();
		HtmlPage page = getPage(webClient, new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/sipm-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre, titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	@Ignore("L'environnement est actuellement arrêté sans perspective proche de redémarrage")
	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testSipmConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg sur l'environnement SIPM...");
		checkStatus("slv2984v", 50609, false);
	}
}
