package ch.vd.uniregctb.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ch.vd.uniregctb.common.WebitTest;

import com.gargoylesoftware.htmlunit.html.HtmlButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class WebitIncontainerTestingJobTest extends WebitTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		webClient.setJavaScriptEnabled(true);
	}

	@Test
	public void testRunBatch() throws Exception {

		final String jobName = "IT-InContainerTestingJob";
		// Démarre le job IT-InContainerTestingJob
		{
			final HtmlPage page = getHtmlPage("/admin/batch.do");
			final HtmlButtonInput button = (HtmlButtonInput) page.getHtmlElementById("start" + jobName);
			assertNotNull(button);
			button.click();
		}

		// L'exécution du rapport devrait être immédiate, mais on attend 2 seconde par mesure de précaution
		Thread.sleep(2000);

		{
			final HtmlPage page = getHtmlPage("/admin/batch.do");
			final HtmlDivision divStatus = (HtmlDivision) page.getHtmlElementById(jobName + "-status");
			final String status = divStatus.asText();
			assertEquals("OK", status);
		}
	}

}
