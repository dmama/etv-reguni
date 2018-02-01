package ch.vd.unireg.evenement.registrefoncier;

/**
 * Les différents types de mutations détectées sur les imports du registre foncier.
 */
public enum TypeMutationRF {
	/**
	 * La mutation est une création d'une nouvelle entité.
	 */
	CREATION,
	/**
	 * La mutation est une modification d'une entité existante.
	 */
	MODIFICATION,
	/**
	 * La mutation est une suppression d'une ou plusieurs entités existantes.
	 */
	SUPPRESSION
}
