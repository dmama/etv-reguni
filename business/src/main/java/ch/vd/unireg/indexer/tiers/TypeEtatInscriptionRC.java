package ch.vd.unireg.indexer.tiers;

/**
 * Valeur stoquée dans l'indexeur par rapport à l'état courant de l'inscription au RC d'une entreprise
 */
public enum TypeEtatInscriptionRC {

	/**
	 * Signifie que l'entreprise est connue au RC et qu'elle y est active
	 */
	ACTIVE,

	/**
	 * Signifie que l'entreprise est connue au RC mais qu'elle n'y est plus active
	 */
	RADIEE
}
