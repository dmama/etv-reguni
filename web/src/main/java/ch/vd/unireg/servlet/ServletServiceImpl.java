package ch.vd.unireg.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.web.context.ServletContextAware;

public class ServletServiceImpl implements ServletService, ServletContextAware {

	private ServletContext servletContext;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void downloadAsFile(String fileName, InputStream is, Integer contentLength, HttpServletResponse response) throws IOException {
		final String mimetype = servletContext.getMimeType(fileName);
		downloadAsFile(fileName, mimetype, is, contentLength, response);
	}

	@Override
	public void downloadAsFile(String fileName, String contentType, InputStream is, Integer contentLength, HttpServletResponse response) throws IOException {
		try (final ServletOutputStream out = response.getOutputStream()) {
			response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

			// On veut que la réponse provoque un téléchargement de fichier
			response.setContentType(contentType);
			if (contentLength != null) {
				response.setContentLength(contentLength);
			}
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + '\"');

			// Copie le fichier dans le flux de réponse de manière continue, pour éviter d'avoir tout le fichier en mémoire.
			IOUtils.copy(is, out);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void downloadAsFile(String fileName, byte[] bytes, HttpServletResponse response) throws IOException {
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			downloadAsFile(fileName, in, bytes.length, response);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void downloadAsFile(String fileName, String content, HttpServletResponse response) throws IOException {
		downloadAsFile(fileName, content.getBytes("ISO-8859-1"), response);
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
}
