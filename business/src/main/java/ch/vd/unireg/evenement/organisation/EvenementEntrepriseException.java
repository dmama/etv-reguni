package ch.vd.unireg.evenement.organisation;

import org.apache.commons.lang3.StringUtils;

/**
 * Exception lancée par le traitement des événements entreprise unitaires, et qui
 * détermine le remplissage d'une queue d'erreur
 */
public class EvenementEntrepriseException extends Exception {

	private static final long serialVersionUID = -268076512760766021L;

    public EvenementEntrepriseException(String message) {
		super(message);
	}

	public EvenementEntrepriseException(String message, Throwable cause) {
		super(message, cause);
	}

	public EvenementEntrepriseException(Throwable cause) {
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
