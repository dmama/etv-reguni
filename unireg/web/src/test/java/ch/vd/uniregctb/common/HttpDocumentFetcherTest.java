package ch.vd.uniregctb.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class HttpDocumentFetcherTest extends WebTest {

	private HttpDocumentFetcher fetcher;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.fetcher = getBean(HttpDocumentFetcher.class, "httpDocumentFetcher");
	}

	@Test
	public void testCodeRetour404() throws Exception {
		final URL bidon = new URL("http://spip/tubidu");
		try (HttpDocumentFetcher.HttpDocument doc = fetcher.fetch(bidon, null)) {
			Assert.fail("Cette URL existe mainenant ?");
		}
		catch (HttpDocumentFetcher.HttpDocumentClientException e) {
			Assert.assertEquals(404, e.getErrorCode());
			Assert.assertEquals("Not Found", e.getErrorMessage());
		}
	}

	@Test
	public void testCodeRetour200() throws Exception {
		final URL blog = new URL("http://spip");
		try (HttpDocumentFetcher.HttpDocument doc = fetcher.fetch(blog, null)) {
			Assert.assertEquals("text/html; charset=UTF-8", doc.getContentType());
			Assert.assertNotNull(doc.getContent());
		}
	}

	@Test
	public void testRecupDocument() throws Exception {
		final URL docUrl = new URL("http://www.vd.ch/fileadmin/user_upload/organisation/dfin/aci/fichiers_pdf/Bareme_revenu_2012_14.pdf");
		try (HttpDocumentFetcher.HttpDocument doc = fetcher.fetch(docUrl, null)) {
			Assert.assertEquals("application/pdf", doc.getContentType());

			final Integer contentLength = doc.getContentLength();
			Assert.assertNotNull(contentLength);

			final File file = File.createTempFile("test-recup-doc", null);
			file.deleteOnExit();

			try (InputStream in = doc.getContent()) {
				Assert.assertNotNull(in);
				try (FileOutputStream out = new FileOutputStream(file)) {
					IOUtils.copy(in, out);
				}
				Assert.assertEquals((long) contentLength, file.length());
			}
		}
	}

	@Test
	public void testMauvaisProtocole() throws Exception {
		final URL ftp = new URL("ftp://toto.edu");
		try (HttpDocumentFetcher.HttpDocument doc = fetcher.fetch(ftp, null)) {
			Assert.fail("Le protocole FTP ne devrait pas être supporté...");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("URL non supportée : " + ftp, e.getMessage());
		}
	}

	@Test
	public void testConnectionRefused() throws Exception {
		final URL url = new URL("http://localhost:53");
		try (HttpDocumentFetcher.HttpDocument doc = fetcher.fetch(url, null)) {
			Assert.fail("La machine locale a son port 53 - serveur DNS - ouvert?");
		}
		catch (ConnectException e) {
			// Apparemment, le passage du JDK 1.8.101 à 1.8.111 change le message de "Connection refused" à "Connection refused (Connection refused)"
			Assert.assertTrue(e.getMessage(), e.getMessage().contains("Connection refused"));
		}
	}

	@Test
	public void testExtractionFilename() throws Exception {
		Assert.assertNull(HttpDocumentFetcherImpl.extractFilename(null));
		Assert.assertNull(HttpDocumentFetcherImpl.extractFilename(""));
		Assert.assertNull(HttpDocumentFetcherImpl.extractFilename("attachment"));
		Assert.assertNull(HttpDocumentFetcherImpl.extractFilename("attachment; filename=\"\""));
		Assert.assertNull(HttpDocumentFetcherImpl.extractFilename("attachment; filename=\"\"; toto=12"));
		Assert.assertNull(HttpDocumentFetcherImpl.extractFilename("attachment; filename=\"   \"; toto=12"));

		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("inline;filename=myfile.txt"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("inline;filename=\"myfile.txt\""));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment;filename=\"myfile.txt\""));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment ; filename=myfile.txt"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment ; filename=\"myfile.txt\""));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment ; filename = myfile.txt"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment ; filename = \"myfile.txt\""));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment ; filename = myfile.txt;toto"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment ; filename = myfile.txt ;toto"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment ; filename = \"myfile.txt\";toto"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcherImpl.extractFilename("attachment ; filename = \"myfile.txt\" ;toto"));
	}
}
