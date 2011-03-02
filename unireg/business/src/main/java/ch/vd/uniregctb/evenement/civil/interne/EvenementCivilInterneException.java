package ch.vd.uniregctb.evenement.civil.interne;

/**
 * Cette exception peut être lancée lors de l'initialisation de l'adaptateur avec les données de l'événement civil.
 * 
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public class EvenementCivilInterneException extends Exception {

	/**
	 * serial Version UID
	 */
	private static final long serialVersionUID = -5953181869732373926L;

	/**
	 * Constructeur.
	 * @param msg le message décrivant l'exception
	 */
	public EvenementCivilInterneException(String msg) {
		super(msg);
	}

	/**
	 * Constructeur avec une exception sous-jascente.
	 * @param cause	l'exception qui est à l'origine du problème
	 */
	public EvenementCivilInterneException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructeur avec une exception sous-jascente et un message.
	 * @param msg le message décrivant l'exception
	 * @param cause	l'exception qui est à l'origine du problème
	 */
	public EvenementCivilInterneException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
