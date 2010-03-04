package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.validation.ValidationResults;

/**
 * Exception levée lorsqu'une adresse ne peut être calculée en raison de problème de données (incohérence, données manquantes, ...)
 */
public class AdresseDataException extends AdresseException {

	private static final long serialVersionUID = 2784065580972326086L;

	public AdresseDataException() {
		super();
	}

	public AdresseDataException(String message) {
		super(message);
	}

	public AdresseDataException(ValidationResults results) {
		super(buildErrorsMessage(results));
	}

	public AdresseDataException(String message, ValidationResults results) {
		super(message + " : " + buildErrorsMessage(results));
	}

	private static String buildErrorsMessage(ValidationResults results) {
		final StringBuilder b = new StringBuilder();
		boolean first = true;
		for (String e : results.getErrors()) {
			if (!first) {
				b.append('\n');
			}
			b.append(e);
			first = false;
		}
		return b.toString();
	}

	public AdresseDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public AdresseDataException(Throwable cause) {
		super(cause);
	}
}