package ch.vd.unireg.fourreNeutre;

import ch.vd.unireg.tiers.Tiers;

public interface FourreNeutreHelper {
	/**
	 * Retourne la première période d'impression valide pour les PP
	 * @return la période paramétrée dans les propriétés
	 */
	Integer getPremierePeriodePP();

	/**
	 * * Retourne la première période d'impression valide pour les PM
	 * @return la période paramétrée dans les propriétés
	 */
	Integer getPremierePeriodePM();

	/**
	 * * Retourne la première période d'impression valide pour l'impot source
	 * @return la période paramétrée dans les propriétés
	 */
	Integer getPremierePeriodeIS();

	/**
	 * Indique si le iters dont l'identifiant est passé en paramètre appartient à une population pour laquelle on peut emettre une fourre neutre
	 * @param tiers le tiers à analyser
	 * @return true si on peut emettre une fourre neutre sur ce tiers, false si non
	 */
	boolean isTiersAutorisePourFourreNeutre(Tiers tiers);
}
