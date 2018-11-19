package ch.vd.unireg.type;

/**
 * Le type de délai d'une déclaration d'impôt.
 */
public enum TypeDelaiDeclaration {
	/**
	 * Le délai a été créé automatiquement à la création de la déclaration d'impôt.
	 */
	IMPLICITE,
	/**
	 * Le délai a été créé explicitement suite à la demande d'un contribuable ou d'un mandataire.
	 */
	EXPLICITE
}
