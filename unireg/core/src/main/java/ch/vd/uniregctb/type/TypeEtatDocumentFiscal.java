/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * Typologie des états de déclaration
 */
public enum TypeEtatDocumentFiscal {

	/**
	 * Déclaration émise, en attente de retour depuis le tiers. C'est le premier état de toute déclaration.
	 */
	EMISE("émise"),

	/**
	 * Une sommation a été émise pour la déclaration.
	 */
	SOMMEE("sommée"),

	/**
	 * Un rappel a été émis pour la déclaration.
	 */
	RAPPELEE("rappelée"),

	/**
	 * Une notification d'échéance (= qui ouvre la porte à la taxation d'office) a été émise pour la déclaration
	 */
	ECHUE("échue"),

	/**
	 * Déclaration suspendue, aucun rappel, aucune sommation ni échéance ne doit pouvoir être généré
	 * tant qu'un tel état non-annulé existe
	 */
	SUSPENDUE("suspendue"),

	/**
	 * Déclaration retournée (= depuis le tiers)
	 */
	RETOURNEE("retournée");

	private final String description;

	TypeEtatDocumentFiscal(String description) {
		this.description = description;
	}

	public String description() {
		return description;
	}
}
