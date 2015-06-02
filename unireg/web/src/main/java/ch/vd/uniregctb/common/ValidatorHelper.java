package ch.vd.uniregctb.common;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public interface ValidatorHelper {

	/**
	 * Ajoute une erreur de validation à l'ensemble donné si le sexe de la personne physique donnée est inconnu
	 * @param pp la personne physique à tester
	 * @param vr le container de l'éventuelle erreur générée
	 */
	void validateSexeConnu(PersonnePhysique pp, ValidationResults vr);

	/**
	 * Ajoute une erreur de validation à l'ensemble donné si la personne physique ne peut pas se marier à la date prévue
	 * (par exemple parce que déjà mariée)
	 * @param pp la personne physique à tester
	 * @param dateMariagePrevu date prévue pour le mariage
	 * @param vr le container de l'éventuelle erreur générée
	 */
	void validatePretPourMariage(PersonnePhysique pp, RegDate dateMariagePrevu, ValidationResults vr);
}
