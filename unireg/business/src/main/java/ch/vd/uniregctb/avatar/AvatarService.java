package ch.vd.uniregctb.avatar;

import ch.vd.uniregctb.tiers.Tiers;

public interface AvatarService {

	/**
	 * Récupère l'avatar pour le tiers donné
	 * @param tiers tiers dont on veut récupérer l'avatar
	 * @param withLink <code>true</code> si l'image doit être celle d'un lien
	 * @return données de l'avatar
	 */
	ImageData getAvatar(Tiers tiers, boolean withLink);

	/**
	 * Récupère l'avatar pour le type donné
	 * @param type type d'avatar (surchargé ou pré-calculé)
	 * @param withLink <code>true</code> si l'image doit être celle d'un lien
	 * @return données de l'avatar
	 */
	ImageData getAvatar(TypeAvatar type, boolean withLink);

	/**
	 * @param tiers tiers quelconque
	 * @return le type d'avatar correspondant
	 */
	TypeAvatar getTypeAvatar(Tiers tiers);
}
