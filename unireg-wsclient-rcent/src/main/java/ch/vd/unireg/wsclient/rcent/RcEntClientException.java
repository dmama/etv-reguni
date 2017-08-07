package ch.vd.unireg.wsclient.rcent;

import javax.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class RcEntClientException extends RuntimeException {

	/**
	 * L'URL appelée par l'appel en erreur.
	 */
	@Nullable
	private final String url;

	/**
	 * Ce lien est transient : comme ça, dans le log de l'application avant éventuelle sérialisation, on le voit,
	 * mais il n'est pas transmis plus loin (au travers d'un appel SpringRemoting, par exemple)
	 */
	private final transient Throwable cause;

	/**
	 * La liste des erreurs renvoyées par RCEnt
	 */
	private List<RcEntClientErrorMessage> errors;

	/**
	 * Construit une exception spécifique avec un message clair et concis à partir d'une ServerWebApplicationException (qui expose la réponse complète dans son message).
	 *
	 * @param e une exception
	 */
	public RcEntClientException(WebApplicationException e, @Nullable List<RcEntClientErrorMessage> errors) {
		this(buildShortMessage(e, errors), e);
		this.errors = errors;
	}

	/**
	 * Méthode utilisée dans les tests pour vérifier le comportement de la cause transiente sans avoir à
	 * s'emm...er avec la construction d'une exception CXF valide
	 * @param shortMessage le message qui restera
	 * @param cause la cause de l'exception, que l'on ne transmettra pas plus loin en cas de sérialisation
	 */
	protected RcEntClientException(String shortMessage, Throwable cause) {
		super(shortMessage);
		this.url = URLKeeperInterceptor.getLastUrl();
		this.cause = cause;
		this.errors = null;
	}

	private static String buildShortMessage(WebApplicationException e, @Nullable List<RcEntClientErrorMessage> errors) {
		final StringBuilder s = new StringBuilder();
		s.append("Status ").append(e.getResponse().getStatus());
		final String title = extractMessage(errors);
		if (title != null) {
			s.append(" (").append(title).append(")");
		}
		return s.toString();
	}

	protected static String extractMessage(List<RcEntClientErrorMessage> errors) {
		if (errors == null || errors.isEmpty()) {
			return null;
		}

		final StringBuilder message = new StringBuilder();
		for (RcEntClientErrorMessage error : errors) {
			if (message.length() > 0) {
				message.append(" | ");
			}
			message.append(error.getCode());
			message.append(": ");
			message.append(error.getMessage());
		}
		return message.toString();
	}

	@Override
	public Throwable getCause() {
		return cause != null ? cause : super.getCause();
	}

	/**
	 * @return la liste des erreurs rapportées, ou {@link Collections#emptyList()} s'il n'y en a pas.
	 */
	public List<RcEntClientErrorMessage> getErrors() {
		if (errors == null) {
			return Collections.emptyList();
		}
		return errors;
	}

	@Override
	public String toString() {
		return super.toString() + " (when calling " + url + ")";
	}
}
