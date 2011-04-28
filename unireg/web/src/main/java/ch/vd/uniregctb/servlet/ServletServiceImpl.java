package ch.vd.uniregctb.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

public class ServletServiceImpl implements ServletService, ServletContextAware {

	private static final Logger LOGGER = Logger.getLogger(ServletServiceImpl.class);

	private static final int BUFFER_SIZE = 4096;

	private ServletContext servletContext;

	/**
	 * {@inheritDoc}
	 */
	public void downloadAsFile(String fileName, InputStream is, Integer contentLength, HttpServletResponse response) throws IOException {

		ServletOutputStream out = response.getOutputStream();
		response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

		// On veut que la réponse provoque un téléchargement de fichier
		String mimetype = servletContext.getMimeType(fileName);
		response.setContentType(mimetype);
		if (contentLength != null) {
			response.setContentLength(contentLength);
		}
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

		// Copie le fichier dans le flux de réponse de manière continue, pour éviter d'avoir tout le fichier en mémoire.
		stream(is, out);
	}

	/**
	 * {@inheritDoc}
	 */
	public void downloadAsFile(String fileName, byte[] bytes, HttpServletResponse response) throws IOException {
		final InputStream in = new ByteArrayInputStream(bytes);
		try {
			downloadAsFile(fileName, in, bytes.length, response);
		}
		finally {
			in.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void downloadAsFile(String fileName, String content, HttpServletResponse response) throws IOException {
		downloadAsFile(fileName, content.getBytes("ISO-8859-1"), response);
	}

	/**
	 * Copy the contents of the given InputStream to the given OutputStream, and flush every BUFFER_SIZE. Closes both streams when done.
	 * <p>
	 * Note: copied-pasted code from FileCopyUtils.copy + flush() added in loop
	 *
	 * @param in
	 *            the stream to copy from
	 * @param out
	 *            the stream to copy to
	 * @return the number of bytes copied
	 * @throws IOException
	 *             in case of I/O errors
	 */
	private static int stream(InputStream in, OutputStream out) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");
		try {
			int byteCount = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				out.flush();
				byteCount += bytesRead;
			}
			out.flush();
			return byteCount;
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ex) {
				LOGGER.warn("Could not close InputStream", ex);
			}
			try {
				out.close();
			}
			catch (IOException ex) {
				LOGGER.warn("Could not close OutputStream", ex);
			}
		}
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
}
