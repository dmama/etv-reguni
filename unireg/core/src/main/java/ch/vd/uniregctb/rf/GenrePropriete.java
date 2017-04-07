package ch.vd.uniregctb.rf;

public enum GenrePropriete {
	/**
	 * Régime de propriété utilisé dans le cas où <b>une personne</b> possède l'intégralité d'un bien.
	 */
	INDIVIDUELLE,
	/**
	 * Régime de propriété utilisé dans le cas où <b>des personnes distinctes</b> ou <b>des immeubles distincts</b> possèdent un bien commun. Par exemple : Monsieur Michaud et Madame Martin, voisins de leurs états, s'entendent pour acheter
	 * en copropriété une parcelle avoisinante. Le bien est découpé en parts, et chaque personne dispose d'une part déterminée.
	 */
	COPROPRIETE,
	/**
	 * Régime de propriété utilisé dans le cas où <b>une communauté de personnes</b> (une hoirie, un couple, ...) possède un bien. Par exemple : Mesdames Dupond, Dupneu et Duboulon, filles de feu Madame et
	 * Monsieur Dumoulin, reçoivent en héritage la villa de leurs parents. Le bien n'est pas découpé en parts.
	 */
	COMMUNE,
	/**
	 * Régime de propriété utilisé pour les droits entre <b>un immeuble PPE</b> et son immeuble de base.
	 */
	PPE,
	/**
	 * Régime de propriété utilisé dans le cas d'<b>un immeuble</b> qui possède un ou plusieurs autres immeubles (hors PPE).
	 */
	FONDS_DOMINANT
}
