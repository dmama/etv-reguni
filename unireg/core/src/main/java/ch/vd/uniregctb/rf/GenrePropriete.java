package ch.vd.uniregctb.rf;

public enum GenrePropriete {
	/**
	 * Type de propriété utilisé dans le cas où <b>une personne</b> possède l'intégralité d'un bien.
	 */
	INDIVIDUELLE,
	/**
	 * Type de propriété utilisé dans le cas où <b>des personnes distinctes</b> possèdent un bien commun. Par exemple : Monsieur Michaud et Madame Martin, voisins de leurs états, s'entendent pour acheter
	 * en copropriété une parcelle avoisinante. Le bien est découpé en parts, et chaque personne dispose d'une part déterminée.
	 */
	COPROPRIETE,
	/**
	 * Type de propriété utilisé dans le cas où <b>une communauté de personnes</b> (une hoirie, un couple, ...) possède un bien. Par exemple : Mesdames Dupond, Dupneu et Duboulon, filles de feu Madame et
	 * Monsieur Dumoulin, reçoivent en héritage la villa de leurs parents. Le bien n'est pas découpé en parts.
	 */
	COMMUNE
}
