package ch.vd.uniregctb.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Classe utilitaire pour récupérer un document, étant connue son URL pérenne d'accès
 */
public abstract class HttpDocumentFetcher {

	private static final String HTTP_CONTENT_TYPE = "Content-Type";
	private static final String HTTP_CONTENT_LENGTH = "Content-Length";
	private static final String HTTP_CONTENT_DISPOSITION = "Content-Disposition";

	/**
	 * Document renvoyé par la méthode {@link #fetch}
	 */
	public static final class HttpDocument {

		/**
		 * Type du contenu
		 */
		private final String contentType;

		/**
		 * Longueur (en bytes) du contenu (si celle-ci est connue)
		 */
		private final Integer contentLength;

		/**
		 * Si fournie dans les headers HTTP, un nom de fichier proposé pour le contenu
		 */
		private final String proposedContentFilename;

		/**
		 * Flux duquel on peut récupérer le contenu
		 */
		private InputStream content;

		private HttpDocument(String contentType, @Nullable Integer contentLength, @Nullable String proposedFilename, InputStream content) {
			this.contentType = contentType;
			this.contentLength = contentLength;
			this.content = content;
			this.proposedContentFilename = proposedFilename;
		}

		public String getContentType() {
			return contentType;
		}

		@Nullable
		public Integer getContentLength() {
			return contentLength;
		}

		@Nullable
		public String getProposedContentFilename() {
			return proposedContentFilename;
		}

		public InputStream getContent() {
			return content;
		}
	}

	/**
	 * Exception qui encapsule une erreur transmise par la réponse HTTP de l'appel
	 */
	public static class HttpDocumentException extends Exception {
		private final int errorCode;
		private final String errorMessage;

		public HttpDocumentException(int errorCode, String errorMessage) {
			this.errorCode = errorCode;
			this.errorMessage = errorMessage;
		}

		public int getErrorCode() {
			return errorCode;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		@Override
		public String getMessage() {
			return String.format("%s (%d)", errorMessage, errorCode);
		}
	}

	/**
	 * Exception lancée pour les erreurs 4xx
	 */
	public static class HttpDocumentClientException extends HttpDocumentException {
		public HttpDocumentClientException(int errorCode, String errorMessage) {
			super(errorCode, errorMessage);
		}
	}

	/**
	 * Exception lancée pour les erreurs 5xx
	 */
	public static class HttpDocumentServerException extends HttpDocumentException {
		public HttpDocumentServerException(int errorCode, String errorMessage) {
			super(errorCode, errorMessage);
		}
	}

	/**
	 * Récupère le document dont l'URL est passée en paramètre
	 * @param url URL d'accès au document recherché
	 * @return le document trouvé
	 * @throws IOException en cas d'erreur de communication
	 * @throws ch.vd.uniregctb.utils.HttpDocumentFetcher.HttpDocumentException si la réponse HTTP n'est pas 200-OK
	 */
	public static HttpDocument fetch(URL url) throws IOException, HttpDocumentException {
		return fetch(url.openConnection());
	}

	/**
	 * Récupère le document dont l'URL est passée en paramètre
	 * @param url URL d'accès au document recherché
	 * @param proxy proxy à utiliser pour accéder à l'URL
	 * @return le document trouvé
	 * @throws IOException en cas d'erreur de communication
	 * @throws ch.vd.uniregctb.utils.HttpDocumentFetcher.HttpDocumentException si la réponse HTTP n'est pas 200-OK
	 */
	public static HttpDocument fetch(URL url, Proxy proxy) throws IOException, HttpDocumentException {
		return fetch(url.openConnection(proxy));
	}

	/**
	 * Récupère les données fournies au travers de la connexion donnée
	 * @param con connexion à utiliser
	 * @return le document trouvé
	 * @throws IOException en cas d'erreur de communication
	 * @throws ch.vd.uniregctb.utils.HttpDocumentFetcher.HttpDocumentException si la réponse HTTP n'est pas 200-OK
	 * @throws IllegalArgumentException si la connexion donnée n'est pas une connection HTTP
	 */
	private static HttpDocument fetch(URLConnection con) throws IOException, HttpDocumentException {
		if (con instanceof HttpURLConnection) {
			final HttpURLConnection httpCon = (HttpURLConnection) con;
			final int responseCode = httpCon.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				final String responseMessage = httpCon.getResponseMessage();
				if (responseCode / 100 == 4) {
					throw new HttpDocumentClientException(responseCode, responseMessage);
				}
				else if (responseCode / 100 == 5) {
					throw new HttpDocumentServerException(responseCode, responseMessage);
				}
				else {
					throw new HttpDocumentException(responseCode, responseMessage);
				}
			}

			final String lengthHeader = httpCon.getHeaderField(HTTP_CONTENT_LENGTH);
			final Integer length = lengthHeader != null && !lengthHeader.isEmpty() ? Integer.parseInt(lengthHeader) : null;
			final String contentType = httpCon.getHeaderField(HTTP_CONTENT_TYPE);
			final String proposedFilename = extractFilename(httpCon.getHeaderField(HTTP_CONTENT_DISPOSITION));
			return new HttpDocument(contentType, length, proposedFilename, httpCon.getInputStream());
		}
		else {
			throw new IllegalArgumentException("Seules les URL au protocole HTTP sont supportées : " + con.getURL());
		}
	}

	private static final Pattern PATTERN_EXTRACTION_FILENAME = Pattern.compile(".*;\\s*filename\\s*=\\s*([^;]+).*");
	private static final Pattern PATTERN_EXCLUSION_QUOTES = Pattern.compile("\"(.*)\"\\s*");

	@Nullable
	protected static String extractFilename(@Nullable String contentDispositionField) {
		final String filename;
		if (StringUtils.isNotBlank(contentDispositionField)) {
			final Matcher filenameMatcher = PATTERN_EXTRACTION_FILENAME.matcher(contentDispositionField);
			if (filenameMatcher.matches()) {
				final String group = filenameMatcher.group(1);
				final Matcher quotesMatcher = PATTERN_EXCLUSION_QUOTES.matcher(group);
				final String extractedValue;
				if (quotesMatcher.matches()) {
					extractedValue = quotesMatcher.group(1);
				}
				else {
					extractedValue = StringUtils.trimToNull(group);
				}
				filename = StringUtils.isBlank(extractedValue) ? null : extractedValue;
			}
			else {
				filename = null;
			}
		}
		else {
			filename = null;
		}
		return filename;
	}
}
