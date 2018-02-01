package ch.vd.unireg.common;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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

		final HttpGet method = new HttpGet(url.toExternalForm());
		try {
			// config de la requête
			final RequestConfig.Builder configBuilder = RequestConfig.custom();
			if (timeoutms != null) {
				configBuilder.setSocketTimeout(timeoutms);
				configBuilder.setConnectTimeout(timeoutms);
				configBuilder.setConnectionRequestTimeout(timeoutms);
			}
			final RequestConfig config = configBuilder.build();
			final CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();

			// on exécute la requête
			final CloseableHttpResponse response;
			try {
				response = client.execute(method);
			}
			catch (ClientProtocolException e) {
				throw new IllegalArgumentException("URL non supportée : " + url, e);
			}

			// gestion d'erreur
			final StatusLine statusLine = response.getStatusLine();
			final int responseCode = statusLine.getStatusCode();
			if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
				final String responseMessage = statusLine.getReasonPhrase();
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

			// récupération du contenu du fichier
			final Integer length;
			if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
				length = 0;
			}
			else {
				final String lengthHeader = extractResponseString(response, HTTP_CONTENT_LENGTH);
				length = lengthHeader != null && !lengthHeader.isEmpty() ? Integer.parseInt(lengthHeader) : null;
			}
			if (length == null || length > 0) {
				final String contentType = extractResponseString(response, HTTP_CONTENT_TYPE);
				final String proposedFilename = extractFilename(extractResponseString(response, HTTP_CONTENT_DISPOSITION));
				try (final InputStream content = response.getEntity().getContent()) {
					return new HttpDocument(contentType, length, proposedFilename, content);
				}
			}
			else {
				return null;
			}
		}
		catch (SocketTimeoutException e) {
			throw new HttpDocumentServerException(504, "Gateway timeout: " + e.getMessage());
		}
		finally {
			method.releaseConnection();
		}
	}

	private static String extractResponseString(CloseableHttpResponse response, String headerName) {
		final Header header = response.getFirstHeader(headerName);
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
