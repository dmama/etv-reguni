package ch.vd.uniregctb.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jetbrains.annotations.Nullable;

/**
 * Interface du bean pour récupérer un document, étant connue son URL pérenne d'accès
 */
public interface HttpDocumentFetcher {

	/**
	 * Document renvoyé par la méthode {@link #fetch}<br/>
	 * <b>Ne pas oublier d'appeler la méthode {@link #close} après utilisation</b>
	 */
	final class HttpDocument implements AutoCloseable {

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
		 * Fichier duquel on peut récupérer le contenu
		 */
		private TempFileInputStreamProvider content;

		public HttpDocument(String contentType, @Nullable Integer contentLength, @Nullable String proposedFilename, InputStream content) throws IOException {
			this.contentType = contentType;
			this.contentLength = contentLength;
			this.content = new TempFileInputStreamProvider("ur-doc-", content);
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

		/**
		 * Attention ! Chaque appel ré-ouvre un nouveau flux !
		 * @return le flux depuis lequel on peut récupérer le contenu du document
		 * @throws IOException en cas de problème lors de l'ouverture du flux
		 */
		public InputStream getContent() throws IOException {
			return content.getInputStream();
		}

		/**
		 * Libère les ressources allouées pour le document
		 */
		@Override
		public void close() {
			content.close();
		}
	}

	/**
	 * Exception qui encapsule une erreur transmise par la réponse HTTP de l'appel
	 */
	class HttpDocumentException extends Exception {
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
	class HttpDocumentClientException extends HttpDocumentException {
		public HttpDocumentClientException(int errorCode, String errorMessage) {
			super(errorCode, errorMessage);
		}
	}

	/**
	 * Exception lancée pour les erreurs 5xx
	 */
	class HttpDocumentServerException extends HttpDocumentException {
		public HttpDocumentServerException(int errorCode, String errorMessage) {
			super(errorCode, errorMessage);
		}
	}

	/**
	 * Récupère le document dont l'URL est passée en paramètre
	 * @param url URL d'accès au document recherché
	 * @return le document trouvé (<code>null</code> si la réponse du serveur est une HTTP 204 No Content ou une HTTP 200 OK avec une longueur de contenu 0)
	 * @throws IOException en cas d'erreur de communication
	 * @throws HttpDocumentFetcher.HttpDocumentException si la réponse HTTP n'est pas 200-OK ou 204-No-Content
	 */
	HttpDocument fetch(URL url, Integer timeoutms) throws IOException, HttpDocumentException;
}
