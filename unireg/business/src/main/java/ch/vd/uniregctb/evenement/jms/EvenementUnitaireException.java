package ch.vd.uniregctb.evenement.jms;

/**
 * Exception lancée par le traitement des événements civils unitaires, et qui
 * détermine le remplissage d'une queue d'erreur
 */
public class EvenementUnitaireException extends Exception {

	public EvenementUnitaireException(String message) {
		super(message);
	}

	public EvenementUnitaireException(String message, Throwable cause) {
		super(message, cause);
	}

	public EvenementUnitaireException(Throwable cause) {
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
