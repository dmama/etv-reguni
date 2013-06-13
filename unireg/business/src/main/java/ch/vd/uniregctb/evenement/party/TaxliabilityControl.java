package ch.vd.uniregctb.evenement.party;

import ch.vd.registre.base.date.RegDate;

public interface TaxliabilityControl {
	/**
	 * Permet d'analyser le tiers en fonction des règles spécifiques aux control
	 *
	 * @param tiersId le numéro de tiers dont ont veut analyser l'assujetissement
	 * @param periode
	 *@param date @return le resultat d el'analayse
	 */
	public TaxliabilityControlResult analyse(Long tiersId, Integer periode, RegDate date);
}
