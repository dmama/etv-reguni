package ch.vd.unireg.security;

import org.apache.commons.lang3.StringUtils;

/**
 * Une exception dans la gestion des droits d'accès spécialisée dans la publication de conflits de droits
 */
public class DroitAccesConflitException extends DroitAccesException {

	private final DroitAccesConflit conflit;

	public DroitAccesConflitException(String msg, DroitAccesConflit conflit) {
		super(buildMessage(msg, conflit.toDisplayMessage()));
		this.conflit = conflit;
	}

	private static String buildMessage(String prefix, String conflitMessage) {
		if (StringUtils.isBlank(prefix)) {
			return conflitMessage;
		}
		else {
			return String.format("%s %s", prefix, conflitMessage);
		}
	}

	public DroitAccesConflit getConflit() {
		return conflit;
	}
}
