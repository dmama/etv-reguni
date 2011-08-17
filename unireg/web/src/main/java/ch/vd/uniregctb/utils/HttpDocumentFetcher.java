package ch.vd.uniregctb.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import org.jetbrains.annotations.Nullable;

/**
 * Classe utilitaire pour récupérer un document, étant connue son URL pérenne d'accès
 */
public abstract class HttpDocumentFetcher {

	private static final String HTTP_CONTENT_TYPE = "Content-Type";
	private static final String HTTP_CONTENT_LENGTH = "Content-Length";

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
		private final Long contentLength;

		/**
		 * Flux duquel on peut récupérer le contenu
		 */
		private InputStream content;

		private HttpDocument(String contentType, @Nullable Long contentLength, InputStream content) {
			this.contentType = contentType;
			this.contentLength = contentLength;
			this.content = content;
		}

		public String getContentType() {
			return contentType;
		}

		@Nullable
		public Long getContentLength() {
			return contentLength;
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
			if (httpCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new HttpDocumentException(httpCon.getResponseCode(), httpCon.getResponseMessage());
			}

			final String lengthHeader = httpCon.getHeaderField(HTTP_CONTENT_LENGTH);
			final Long length = lengthHeader != null && !lengthHeader.isEmpty() ? Long.parseLong(lengthHeader) : null;
			final String contentType = httpCon.getHeaderField(HTTP_CONTENT_TYPE);
			return new HttpDocument(contentType, length, httpCon.getInputStream());
		}
		else {
			throw new IllegalArgumentException("Seules les URL au protocole HTTP sont supportées : " + con.getURL());
		}
	}
}
