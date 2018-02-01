package ch.vd.unireg.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

/**
 * @author Raphaël Marmier, 2016-01-22, <raphael.marmier@vd.ch>
 */
public interface TransitionEtatEntrepriseFactory {

	/**
	 * Crée une transition après avoir vérifié sa disponibilité pour des éléments fournis. La transition
	 * encapsule tous les éléments nécessaires à son application sur l'entreprise.
	 *
	 * @param entreprise L'entreprise ciblée par la transition d'état
	 * @param date La date de la transition d'état
	 * @param generation L'indication du type de génération de l'état
	 * @return la transition correspondante, ou null si elle n'est pas disponible
	 */
	TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation);

}
