package ch.vd.uniregctb.print;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64InputStream;

import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.common.StreamUtils;

public class PrintPCLManagerImpl implements PrintPCLManager{

	private static boolean localApp;

	public static boolean isLocalApp() {
		return localApp;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setLocalApp(String localApp) {
		PrintPCLManagerImpl.localApp = "true".equalsIgnoreCase(localApp) || "1".equals(localApp) || "yes".equalsIgnoreCase(localApp);
	}

	/**
	 * Ouvre un flux X-CHVD ou PCL
	 * en fonction du boolean localApp
	 *
	 * @param response
	 * @param pcl
	 * @throws IOException
	 */
	public void openPclStream(HttpServletResponse response, String filenameRadical, byte[] pcl) throws IOException {

		final ServletOutputStream out = response.getOutputStream();
		try {
			response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

			final String actualMimeType = getActualMimeType();
			final String filenameExtension = MimeTypeHelper.getFileExtensionForType(actualMimeType);
			response.setContentType(actualMimeType);
			response.setHeader("Content-disposition", String.format("%s; filename=\"%s%s\"", isAttachmentContent() ? "attachment" : "inline", filenameRadical, filenameExtension));
			response.setHeader( "Pragma", "public" );
			response.setHeader("cache-control", "no-cache");
			response.setHeader("Cache-control", "must-revalidate");

			final ByteArrayInputStream in = new ByteArrayInputStream(pcl);
			try {
				copyToOutputStream(in, out);
				out.flush();
			}
			finally {
				in.close();
			}
		}
		finally {
			out.close();
		}
	}

	private String construitDebutCorpsLocalApp() {
		return "<?xml version=\"1.0\" encoding='" + Charset.defaultCharset().name() + "'?>" +
				"<tasklist name=\"printPCLList\">" +
				"	<task name=\"printPCL\" action=\"print\">" +
				"		<print>" +
				"			<parameter>";
	}

	private String construitFinCorpsLocalApp() {
		return  "			</parameter>" +
				"		</print>" +
				"	</task>" +
				"</tasklist>";
	}


	@Override
	public boolean isAttachmentContent() {
		// en localapp, apparemment, il faut mettre "inline"
		return !isLocalApp();
	}

	@Override
	public String getActualMimeType() {
		return isLocalApp() ? MimeTypeHelper.MIME_CHVD : MimeTypeHelper.MIME_PCL;
	}

	@Override
	public void copyToOutputStream(InputStream in, OutputStream out) throws IOException {
		if (isLocalApp()) {
			out.write(construitDebutCorpsLocalApp().getBytes());

			final InputStream base64Encoder = new Base64InputStream(in, true);
			StreamUtils.copy(base64Encoder, out);

			out.write(construitFinCorpsLocalApp().getBytes());
		}
		else {
			StreamUtils.copy(in, out);
		}
	}
}
