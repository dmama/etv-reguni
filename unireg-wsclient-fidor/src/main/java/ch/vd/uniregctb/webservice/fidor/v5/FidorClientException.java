package ch.vd.uniregctb.webservice.fidor.v5;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class FidorClientException extends RuntimeException {

	/**
	 * Ce lien est transient : comme ça, dans le log de l'application avant éventuelle sérialisation, on le voit,
	 * mais il n'est pas transmis plus loin (au travers d'un appel SpringRemoting, par exemple)
	 */
	private final transient Throwable cause;

	/**
	 * Construit une exception spécifique avec un message clair et concis à partir d'une WebApplicationException (qui expose la réponse complète dans son message).
	 *
	 * @param e une exception
	 */
	public FidorClientException(WebApplicationException e) {
		this(buildShortMessage(e), e);
	}

	/**
	 * Construit une exception spécifique à partir d'une Exception JAXB
	 */
	public FidorClientException(JAXBException e) {
		this(e.getMessage(), e);
	}

	/**
	 * Méthode utilisée dans les tests pour vérifier le comportement de la cause transiente sans avoir à
	 * s'emm...er avec la construction d'une exception CXF valide
	 * @param shortMessage le message qui restera
	 * @param cause la cause de l'exception, que l'on ne transmettra pas plus loin en cas de sérialisation
	 */
	protected FidorClientException(String shortMessage, Throwable cause) {
		super(shortMessage);
		this.cause = cause;
	}

	private static String buildShortMessage(WebApplicationException e) {
		final StringBuilder s = new StringBuilder();
		s.append("Status ").append(e.getResponse().getStatus());
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

	@Override
	public Throwable getCause() {
		return cause != null ? cause : super.getCause();
	}
}
