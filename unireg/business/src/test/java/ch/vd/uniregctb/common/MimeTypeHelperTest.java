package ch.vd.uniregctb.common;

import org.junit.Assert;
import org.junit.Test;

public class MimeTypeHelperTest extends WithoutSpringTest {

	@Test
	public void testSimpleCases() throws Exception {
		Assert.assertEquals(".pcl", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_PCL));
		Assert.assertEquals(".pcl", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_XPCL));
		Assert.assertEquals(".pcl", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_HPPCL));
		Assert.assertEquals(".chvd", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_CHVD));
		Assert.assertEquals(".csv", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_CSV));
		Assert.assertEquals(".txt", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_PLAINTEXT));
		Assert.assertEquals(".html", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_HTML));
		Assert.assertEquals(".pdf", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_PDF));
		Assert.assertEquals(".xml", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_APPXML));
		Assert.assertEquals(".xml", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_XML));
		Assert.assertEquals(".zip", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_ZIP));
		Assert.assertEquals(".doc", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_MSWORD));
		Assert.assertEquals(".tiff", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_TIFF));
		Assert.assertEquals(".afp", MimeTypeHelper.getFileExtensionForType(MimeTypeHelper.MIME_AFP));
	}

	@Test
	public void testUnknownTypes() throws Exception {
		Assert.assertEquals("", MimeTypeHelper.getFileExtensionForType(null));
		Assert.assertEquals("", MimeTypeHelper.getFileExtensionForType("text/abracadabra"));
		Assert.assertEquals("", MimeTypeHelper.getFileExtensionForType("text/abracadabra; charset=ISO-8859-1"));
	}

	@Test
	public void testTextWithCharset() throws Exception {
		Assert.assertEquals(".txt", MimeTypeHelper.getFileExtensionForType("text/plain; charset=UTF-8"));
	}
}
