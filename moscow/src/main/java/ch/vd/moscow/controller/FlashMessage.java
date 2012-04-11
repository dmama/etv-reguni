package ch.vd.moscow.controller;

import org.apache.commons.lang.StringUtils;

/**
 * Un flash message est un cours message (info/warning/error) qui apparaît en haut de l'écran immédiatement après un action de l'utilisateur. Sa particularité est qu'il n'apparaît qu'une seule fois.
 * Ainsi si l'utilisateur rafraîchit la page (F5), le message disparaît de lui-même.
 */
public class FlashMessage {

	private String displayClass;
	private String message;
	private long timeout;

	/**
	 * Spécifie un message d'information
	 *
	 * @param message le message à afficher en haut de la page
	 */
	public void setMessage(String message) {
		setMessage(message, 0L);
	}

	/**
	 * Spécifie un message d'information
	 *
	 * @param message le message à afficher en haut de la page
	 * @param timeout timeout, en millisecondes, au delà duquel le message peut-être à nouveau effacé (0 pour l'absence de timeout)
	 */
	public void setMessage(String message, long timeout) {
		this.displayClass = "flash";
		this.message = message;
		this.timeout = timeout;
	}

	/**
	 * Spécifie un message de warning
	 *
	 * @param message le message à afficher en haut de la page
	 */
	public void setWarning(String message) {
		setWarning(message, 0L);
	}

	/**
	 * Spécifie un message de warning
	 *
	 * @param message le message à afficher en haut de la page
	 * @param timeout timeout, en millisecondes, au delà duquel le message peut-être à nouveau effacé (0 pour l'absence de timeout)
	 */
	public void setWarning(String message, long timeout) {
		this.displayClass = "flash-warning";
		this.message = message;
		this.timeout = timeout;
	}

	/**
	 * Spécifie un message d'erreur
	 *
	 * @param message le message à afficher en haut de la page
	 */
	public void setError(String message) {
		setError(message, 0L);
	}

	/**
	 * Spécifie un message d'erreur
	 *
	 * @param message le message à afficher en haut de la page
	 * @param timeout timeout, en millisecondes, au delà duquel le message peut-être à nouveau effacé (0 pour l'absence de timeout)
	 */
	public void setError(String message, long timeout) {
		this.displayClass = "flash-error";
		this.message = message;
		this.timeout = timeout;
	}

	/**
	 * @return <b>vrai</b> s'il y a un message à afficher
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public boolean isActive() {
		return StringUtils.isNotBlank(message);
	}

	public String getMessage() {
		return message;
	}

	/**
	 * @return la classe Html à utiliser pour afficher le message.
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public String getDisplayClass() {
		return displayClass;
	}

	/**
	 * @return retourne le message mémorisé (ou <b>null</b> s'il n'y en a pas) et oublie-le immédiatement. Une deuxième appel à cette méthode retournera <b>null</b> tant qu'aucun nouveau message n'est
	 *         pas spécifié.
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public String getMessageForDisplay() {
		final String s = message;
		message = null;
		return s;
	}

	/**
	 * @return le temps d'affichage du message, en millisecondes (0 ou moins pour un message indéfiniment affiché)
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public long getTimeout() {
		return timeout;
	}
}
