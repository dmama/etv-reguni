package ch.vd.unireg.wsclient.rcpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;

public class RcPersClientException extends RuntimeException {

	/**
	 * Construit une exception spécifique avec un message clair et concis à partir d'une ServerWebApplicationException (qui expose la réponse complète dans son message).
	 *
	 * @param e une exception
	 */
	public RcPersClientException(ServerWebApplicationException e) {
		super(buildShortMessage(e));
	}

	private static String buildShortMessage(ServerWebApplicationException e) {
		final StringBuilder s = new StringBuilder();
		s.append("Status ").append(e.getStatus());
		final String title = extractTitle(e.getMessage());
		if (title != null) {
			s.append(" (").append(title).append(")");
		}
		return s.toString();
	}

	private static final Pattern TITLE_PATTERN = Pattern.compile(".*?<title>(.*?)</title>.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	protected static String extractTitle(String html) {
		if (StringUtils.isBlank(html)) {
			return null;
		}

		final Matcher matcher = TITLE_PATTERN.matcher(html);
		if (matcher.matches()) {
			return matcher.group(1);
		}

		return null;
	}
}
