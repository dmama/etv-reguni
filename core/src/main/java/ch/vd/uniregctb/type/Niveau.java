/**
 *
 */
package ch.vd.uniregctb.type;

public enum Niveau {
	/**
	 * Utilisé en conjonction avec {@link TypeDroitAcces}, permet d'autoriser l'accès en lecture sur un dossier, ou d'interdire tout accès
	 * sur un dossier.
	 */
	LECTURE,
	/**
	 * Utilisé en conjonction avec {@link TypeDroitAcces}, permet d'autoriser l'accès total (lecture + écriture) sur un dossier, ou
	 * d'interdire l'accès en écriture sur un dossier.
	 */
	ECRITURE
}
