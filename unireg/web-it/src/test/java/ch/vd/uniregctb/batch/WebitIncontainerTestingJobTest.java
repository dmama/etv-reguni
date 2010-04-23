package ch.vd.uniregctb.batch;

import com.gargoylesoftware.htmlunit.html.HtmlButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Test;

import ch.vd.uniregctb.common.WebitTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WebitIncontainerTestingJobTest extends WebitTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		webClient.setJavaScriptEnabled(true);
	}

	@Test(timeout = 20000)
	public void testRunBatch() throws Exception {

		final String jobName = "IT-InContainerTestingJob";
		// Démarre le job IT-InContainerTestingJob
		{
			final HtmlPage page = getHtmlPage("/admin/batch.do");
			final HtmlButtonInput button = (HtmlButtonInput) page.getHtmlElementById("start" + jobName);
			assertNotNull(button);
			button.click();
		}

		// Le démarrage du batch devrait être immédiat, mais on attend 2 secondes par mesure de précaution
		Thread.sleep(2000);

		// Attente de la fin de l'exécution
		while (getStatus(jobName).equals("En cours")) {
			Thread.sleep(100);
		}

		final String status = getStatus(jobName);
		assertEquals("OK", status);
	}

	private String getStatus(String jobName) throws Exception {
		final HtmlPage page = getHtmlPage("/admin/batch.do");
		final HtmlDivision divStatus = (HtmlDivision) page.getHtmlElementById(jobName + "-status");
		return divStatus.asText();
	}
}
