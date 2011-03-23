package ch.vd.uniregctb.supergra;

import org.apache.commons.lang.StringUtils;

/**
 * Un flash message est un cours message (info/warning/error) qui apparaît en haut de l'écran immédiatement après un action de l'utilisateur. Sa particularité est qu'il n'apparaît qu'une seule fois.
 * Ainsi si l'utilisateur rafraîchit la page (F5), le message disparaît de lui-même.
 */
public class FlashMessage {

	private String displayClass;
	private String message;

	/**
	 * Spécifie un message d'information
	 *
	 * @param message le message à afficher en haut de la page
	 */
	public void setMessage(String message) {
		this.displayClass = "flash";
		this.message = message;
	}

	/**
	 * Spécifie un message de warning
	 *
	 * @param message le message à afficher en haut de la page
	 */
	public void setWarning(String message) {
		this.displayClass = "flash-warning";
		this.message = message;
	}

	/**
	 * Spécifie un message d'erreur
	 *
	 * @param message le message à afficher en haut de la page
	 */
	public void setError(String message) {
		this.displayClass = "flash-error";
		this.message = message;
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
}
