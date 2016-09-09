package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;

/**
 * Interface implémentée par les composants d'assujettissement et utilisable
 * pour être capable de dire si une commune est concernée ou pas par un assujettissement donné
 */
public interface AssujettissementSurCommuneAnalyzer {

	/**
	 * @param assujettissement un assujettissement
	 * @return une liste des fors (vaudois) déterminants pour les communes vaudoises concernées
	 */
	List<ForFiscalRevenuFortune> getForsVaudoisDeterminantsPourCommunes(Assujettissement assujettissement);
}
