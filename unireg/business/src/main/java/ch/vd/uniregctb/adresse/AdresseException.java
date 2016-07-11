package ch.vd.uniregctb.adresse;

/**
 * Classe de base des exceptions touchant les adresses fiscales.
 */
public abstract class AdresseException extends Exception {

	public AdresseException() {
	}

	public AdresseException(String message) {
		super(message);
	}

	public AdresseException(String message, Throwable cause) {
		super(message, cause);
	}

	public AdresseException(Throwable cause) {
		super(cause.getMessage(), cause);
	}
}