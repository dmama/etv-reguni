package ch.vd.uniregctb.type;

public enum StatutMenageCommun {

	/**
	 * Le ménage commun est toujours en vigueur
	 */
	EN_VIGUEUR,

	/**
	 * Le ménage commun est terminé suite au décès d'au moins un des membres
	 */
	TERMINE_SUITE_DECES,

	/**
	 * Le ménage commun est terminé suite à la séparation des membres
	 * (divorce, dissolution de partenariat, séparation)
	 */
	TERMINE_SUITE_SEPARATION

}

