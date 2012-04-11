/**
 *
 */
package ch.vd.uniregctb.type;

public enum TypeDroitAcces {
	/**
	 * Utilisé en conjonction avec {@link TypeAcces}, donne l'autorisation à un opérateur d'accéder à un dossier. Tous les autres opérateurs
	 * perdent cette autorisation de manière implicite.
	 */
	AUTORISATION,
	/**
	 * Utilisé en conjonction avec {@link TypeAcces}, interdit un opérateur d'accéder à un dossier. Les droits des autres opérateurs ne sont
	 * pas touchés.
	 */
	INTERDICTION
}
