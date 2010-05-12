package ch.vd.uniregctb.evenement.jms;

/**
 * Exception lancée par le traitement des événements civils unitaires, et qui
 * détermine le remplissage d'une queue d'erreur
 */
public class EvenementCivilException extends Exception {

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
		if (getCause() != null) {
			return getCause().getMessage();
		}
		else {
			return super.getMessage();
		}
	}
}
