package ch.vd.uniregctb.evenement.civil.common;

import org.apache.commons.lang.StringUtils;

/**
 * Exception lancée par le traitement des événements civils unitaires, et qui
 * détermine le remplissage d'une queue d'erreur
 */
public class EvenementCivilException extends Exception {

    private static final long serialVersionUID = -2525541800388242452L;

    public EvenementCivilException(String message) {
		super(message);
	}

	public EvenementCivilException(String message, Throwable cause) {
		super(message, cause);
	}

	public EvenementCivilException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage() {
        if (StringUtils.isNotBlank(super.getMessage())) {
            return super.getMessage();
        } else if (getCause() != null) {
            return getCause().getMessage();
        } else {
			return super.getMessage();
		}
	}
}
