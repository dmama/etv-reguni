package ch.vd.watchdog;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import com.gargoylesoftware.htmlunit.WebClient;
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
		final WebClient webClient = loginIamValidation();
		HtmlPage page = getPage(webClient, new URL("https://validation.portail.etat-de-vaud.ch/fiscalite/dev-unireg/web/"));
		assertNotNull(page);
		String titre = page.getTitleText();
		assertTrue(titre, titre.equalsIgnoreCase("Recherche des tiers") || titre.equalsIgnoreCase("Sélection de l'OID de travail"));
	}

	@Test(timeout = WatchDogTest.TIMEOUT)
	public void testIntegrationPostProductionConnectivite() throws Exception {
		LOGGER.info("Vérification de la connectivité de Unireg en intégration de post-production...");

		// on ne vérifie plus le service SIPF en i2, ils ne sont pas assez souvent là...
		checkStatus("ssv0309v", 54609, false, "serviceBVRPlus");
	}
}
