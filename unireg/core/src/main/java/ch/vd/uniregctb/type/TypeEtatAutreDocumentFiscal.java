package ch.vd.uniregctb.type;

/**
 * Les états possibles d'un "autre document fiscal"
 */
public enum TypeEtatAutreDocumentFiscal {

	/**
	 * Le document a été envoyé
	 */
	EMIS,

	/**
	 * Une réponse a été reçue pour le document précédemment envoyé
	 */
	RETOURNE,

	/**
	 * Une lettre de rappel a été envoyée
	 */
	RAPPELE
}
