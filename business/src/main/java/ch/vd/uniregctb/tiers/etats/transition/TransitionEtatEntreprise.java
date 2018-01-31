package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * Représente l'opération de transition d'un état à l'autre de l'entreprise.
 *
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public interface TransitionEtatEntreprise {

	/**
	 * Passe une entreprise d'un état à un autre.
	 *
	 * <h4>Note:</h4>
	 * <ul>
	 *     <li>
	 *         C'est une méthode à effet de bord: le DAO est appelé et la transition est effectuée sur l'entreprise passée en paramètre
	 *     </li>
	 * </ul>
	 *
	 * @return Le nouvel état de l'entreprise.
	 */
	EtatEntreprise apply();

	/**
	 * @return le type de l'état que l'entreprise aura à l'arrivée, c'est à dire après l'application de cette transition.
	 */
	TypeEtatEntreprise getTypeDestination();

}
