package ch.vd.uniregctb.common;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Bean pour récupérer un document, étant connue son URL pérenne d'accès
 */
public class HttpDocumentFetcherImpl implements HttpDocumentFetcher {

	private static final String HTTP_CONTENT_TYPE = HttpHeaders.CONTENT_TYPE;
	private static final String HTTP_CONTENT_LENGTH = HttpHeaders.CONTENT_LENGTH;
	private static final String HTTP_CONTENT_DISPOSITION = "Content-Disposition";

	/**
	 * Récupère le document dont l'URL est passée en paramètre
	 * @param url URL d'accès au document recherché
	 * @return le document trouvé (<code>null</code> si la réponse du serveur est une HTTP 204 No Content ou une HTTP 200 OK avec une longueur de contenu 0)
	 * @throws IOException en cas d'erreur de communication
	 * @throws HttpDocumentFetcher.HttpDocumentException si la réponse HTTP n'est pas 200-OK ou 204-No-Content
	 */
	public HttpDocument fetch(URL url, Integer timeoutms) throws IOException, HttpDocumentException {
		final GetMethod method;
		try {
			method = new GetMethod(url.toExternalForm());
			if (timeoutms != null) {
				method.getParams().setSoTimeout(timeoutms);
			}
		}
		catch (IllegalStateException e) {
			throw new IllegalArgumentException("URL non supportée : " + url, e);
		}

		try {
			final SimpleHttpConnectionManager httpConnectionManager = new SimpleHttpConnectionManager(true);
			final HttpClient client = new HttpClient(httpConnectionManager);
			if (timeoutms != null) {
				client.getParams().setConnectionManagerTimeout(timeoutms);
			}
			final int responseCode = client.executeMethod(method);
			if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
				final String responseMessage = method.getStatusText();
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

			final Integer length;
			if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
				length = 0;
			}
			else {
				final String lengthHeader = extractResponseString(method, HTTP_CONTENT_LENGTH);
				length = lengthHeader != null && !lengthHeader.isEmpty() ? Integer.parseInt(lengthHeader) : null;
			}
			if (length == null || length > 0) {
				final String contentType = extractResponseString(method, HTTP_CONTENT_TYPE);
				final String proposedFilename = extractFilename(extractResponseString(method, HTTP_CONTENT_DISPOSITION));
				return new HttpDocument(contentType, length, proposedFilename, method.getResponseBodyAsStream());
			}
			else {
				return null;
			}
		}
		catch (ConnectTimeoutException | SocketTimeoutException e) {
			throw new HttpDocumentServerException(504, "Gateway timeout: " + e.getMessage());
		}
		finally {
			method.releaseConnection();
		}
	}

	@Nullable
	private static String extractResponseString(HttpMethod method, String headerName) {
		final Header header = method.getResponseHeader(headerName);
		return header != null ? StringUtils.trimToNull(header.getValue()) : null;
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
