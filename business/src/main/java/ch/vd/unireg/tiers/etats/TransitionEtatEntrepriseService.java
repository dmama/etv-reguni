package ch.vd.unireg.tiers.etats;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.etats.transition.TransitionEtatEntreprise;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

/**
 * Factory pour la création des transitions d'état de l'entreprise.
 *
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public interface TransitionEtatEntrepriseService {

	/**
	 * Génère la liste des transitions effectivement disponibles actuellement pour une entreprise en fonction de ses
	 * caractéristiques et des paramètres.
	 *
	 * @param entreprise l'entreprise ciblée par la transition d'état
	 * @param date la date désirée pour la transition d'état
	 * @param generation l'indication du type de génération de l'état
	 * @return la map des transitions disponibles, classé par type d'état final désiré
	 */
	Map<TypeEtatEntreprise, TransitionEtatEntreprise> getTransitionsDisponibles(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation);

	/**
	 * Détermine et retourne la transition désirée si effectivement disponible pour une entreprise en fonction de ses
	 * caractéristiques et des paramètres.
	 *
	 * @param type le type de la transition désirée
	 * @param entreprise l'entreprise ciblée par la transition d'état
	 * @param date la date désirée pour la transition d'état
	 * @param generation l'indication du type de génération de l'état
	 * @return la transition pour l'état désiré, ou null si indisponible
	 */
	TransitionEtatEntreprise getTransitionVersEtat(TypeEtatEntreprise type, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation);
}
