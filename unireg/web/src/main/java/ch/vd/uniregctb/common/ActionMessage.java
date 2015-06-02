package ch.vd.uniregctb.common;

import org.apache.commons.lang3.StringUtils;

/**
 * Un message global d'erreur ou de warning qui apparaît en haut de l'écran suite à un problème déclanché par une action de l'utilisateur. Sa particularité est qu'il n'apparaît qu'une seule fois.
 * Ainsi si l'utilisateur rafraîchit la page (F5), le message disparaît de lui-même.
 */
public class ActionMessage {

	private String displayClass;
	private String message;

	/**
	 * Spécifie un message de warning
	 *
	 * @param message le message à afficher en haut de la page
	 */
	public void setWarning(String message) {
		this.displayClass = "warn";
		this.message = message;
	}

	/**
	 * Spécifie un message d'erreur
	 *
	 * @param message le message à afficher en haut de la page
	 */
	public void setError(String message) {
		this.displayClass = "err";
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
}
