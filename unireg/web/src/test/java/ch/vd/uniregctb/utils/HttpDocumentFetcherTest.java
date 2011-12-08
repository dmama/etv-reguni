package ch.vd.uniregctb.utils;

import java.net.ConnectException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class HttpDocumentFetcherTest extends WithoutSpringTest {

	@Test
	public void testCodeRetour404() throws Exception {
		final URL bidon = new URL("http://calimero/tubidu");
		try {
			HttpDocumentFetcher.fetch(bidon);
			Assert.fail("Cette URL existe mainenant ?");
		}
		catch (HttpDocumentFetcher.HttpDocumentClientException e) {
			Assert.assertEquals(404, e.getErrorCode());
		}
	}

	@Test
	public void testCodeRetour200() throws Exception {
		final URL blog = new URL("http://calimero/blog");
		final HttpDocumentFetcher.HttpDocument doc = HttpDocumentFetcher.fetch(blog);
		Assert.assertEquals("text/html; charset=UTF-8", doc.getContentType());
		Assert.assertNotNull(doc.getContent());
	}

	@Test
	public void testMauvaisProtocole() throws Exception {
		final URL ftp = new URL("ftp://toto.edu");
		try {
			HttpDocumentFetcher.fetch(ftp);
			Assert.fail("Le protocole FTP ne devrait pas être supporté...");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Seules les URL au protocole HTTP sont supportées : " + ftp, e.getMessage());
		}
	}

	@Test
	public void testConnectionRefused() throws Exception {
		final URL url = new URL("http://localhost:53");
		try {
			HttpDocumentFetcher.fetch(url);
			Assert.fail("La machine locale a son port 53 - serveur DNS - ouvert?");
		}
		catch (ConnectException e) {
			Assert.assertEquals("Connection refused", e.getMessage());
		}
	}

	@Test
	public void testExtractionFilename() throws Exception {
		Assert.assertNull(HttpDocumentFetcher.extractFilename(null));
		Assert.assertNull(HttpDocumentFetcher.extractFilename(""));
		Assert.assertNull(HttpDocumentFetcher.extractFilename("attachment"));
		Assert.assertNull(HttpDocumentFetcher.extractFilename("attachment; filename=\"\""));
		Assert.assertNull(HttpDocumentFetcher.extractFilename("attachment; filename=\"\"; toto=12"));
		Assert.assertNull(HttpDocumentFetcher.extractFilename("attachment; filename=\"   \"; toto=12"));

		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("inline;filename=myfile.txt"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("inline;filename=\"myfile.txt\""));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment;filename=\"myfile.txt\""));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment ; filename=myfile.txt"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment ; filename=\"myfile.txt\""));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment ; filename = myfile.txt"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment ; filename = \"myfile.txt\""));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment ; filename = myfile.txt;toto"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment ; filename = myfile.txt ;toto"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment ; filename = \"myfile.txt\";toto"));
		Assert.assertEquals("myfile.txt", HttpDocumentFetcher.extractFilename("attachment ; filename = \"myfile.txt\" ;toto"));
	}
}
