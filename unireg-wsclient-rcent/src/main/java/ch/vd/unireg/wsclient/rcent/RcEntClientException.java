package ch.vd.unireg.wsclient.rcent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;

public class RcEntClientException extends RuntimeException {

	/**
	 * Ce lien est transient : comme ça, dans le log de l'application avant éventuelle sérialisation, on le voit,
	 * mais il n'est pas transmis plus loin (au travers d'un appel SpringRemoting, par exemple)
	 */
	private final transient Throwable cause;

	/**
	 * Construit une exception spécifique avec un message clair et concis à partir d'une ServerWebApplicationException (qui expose la réponse complète dans son message).
	 *
	 * @param e une exception
	 */
	public RcEntClientException(ServerWebApplicationException e) {
		this(buildShortMessage(e), e);
	}

	/**
	 * Méthode utilisée dans les tests pour vérifier le comportement de la cause transiente sans avoir à
	 * s'emm...er avec la construction d'une exception CXF valide
	 * @param shortMessage le message qui restera
	 * @param cause la cause de l'exception, que l'on ne transmettra pas plus loin en cas de sérialisation
	 */
	protected RcEntClientException(String shortMessage, Throwable cause) {
		super(shortMessage);
		this.cause = cause;
	}

	private static String buildShortMessage(ServerWebApplicationException e) {
		final StringBuilder s = new StringBuilder();
		s.append("Status ").append(e.getStatus());
		final String title = extractMessage(e.getMessage());
		if (title != null) {
			s.append(" (").append(title).append(")");
		}
		return s.toString();
	}

	private static final Pattern MESSAGE_PATTERN = Pattern.compile("<(?:[^:>]+:)?message>([^<]+)</(?:[^:>]+:)?message>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	protected static String extractMessage(String xml) {
		if (StringUtils.isBlank(xml)) {
			return null;
		}

		StringBuilder message = new StringBuilder();

		final Matcher matcher = MESSAGE_PATTERN.matcher(xml);
		while (matcher.find()) {
			if (message.length() > 0) {
				message.append(" | ");
			}
			message.append(matcher.group(1));
		}
		return message.toString();
	}

	@Override
	public Throwable getCause() {
		return cause != null ? cause : super.getCause();
	}
}
