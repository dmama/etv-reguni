package ch.vd.uniregctb.rf;

public enum GenrePropriete {
	/**
	 * Type de propriété utilisé dans le cas où <b>une personne</b> possède l'intégralité d'un bien.
	 */
	INDIVIDUELLE,
	/**
	 * Type de propriété utilisé dans le cas où <b>des personnes distinctes</b> possèdent un bien commun. Par exemple : Monsieur Michaud et Madame Martin, voisins de leurs états, s'entendent pour acheter en
	 * co-propriété une parcelle avoisinante.
	 */
	COPROPRIETE,
	/**
	 * Type de propriété utilisé dans le cas où <b>une communauté de personnes</b> (une hoirie, un couple, ...) possède un bien. Par exemple : Mesdames Dupond, Dupneu et Duboulon, filles de feu Madame et
	 * Monsieur Dumoulin, reçoivent en héritage la villa de leurs parents.
	 */
	COMMUNE,
	/**
	 * Type de propriété utilisé dans le cas d'une <b>propriété par étage</b> (PPE).
	 */
	COLLECTIVE
}
