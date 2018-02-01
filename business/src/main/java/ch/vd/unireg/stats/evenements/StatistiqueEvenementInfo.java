package ch.vd.uniregctb.stats.evenements;

public interface StatistiqueEvenementInfo {

	/**
	 * Renvoie le nom des colonnes
	 */
	String[] getNomsColonnes();

	/**
	 * Renvoie les valeurs (formattées) correspondant aux colonnes
	 */
	String[] getValeursColonnes();
}
