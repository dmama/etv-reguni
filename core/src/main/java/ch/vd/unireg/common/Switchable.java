package ch.vd.unireg.common;

/**
 * Interface pour un élément qui peut être activé ou désactivé
 */
public interface Switchable {

	/**
	 * Pour changer l'état d'activation
	 * @param enabled le nouvel état
	 */
	void setEnabled(boolean enabled);

	/**
	 * @return l'état d'activation actuel
	 */
	boolean isEnabled();
}
