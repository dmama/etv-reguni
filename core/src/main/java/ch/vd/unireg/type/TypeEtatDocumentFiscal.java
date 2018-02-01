/**
 *
 */
package ch.vd.unireg.type;

/**
 * Typologie des états de documents fiscaux. Tous les états ne s'appliquent pas à tous les documents.
 */
public enum TypeEtatDocumentFiscal {

	/**
	 * Document émis, en attente de retour depuis le tiers. C'est le premier état de tout document fiscal.
	 */
	EMIS("émis", "émise"),

	/**
	 * Une sommation a été émise pour le document.
	 */
	SOMME("sommé", "sommée"),

	/**
	 * Un rappel a été émis pour le document.
	 */
	RAPPELE("rappelé", "rappelée"),

	/**
	 * Une notification d'échéance a été émise pour le document. [= qui ouvre la porte pour une déclaration à la taxation d'office]
	 */
	ECHU("échu", "échue"),

	/**
	 * Document suspendu: aucun rappel, aucune sommation ni échéance ne doit pouvoir être généré
	 * tant qu'un tel état non-annulé existe
	 */
	SUSPENDU("suspendu", "suspendue"),

	/**
	 * Document retournée (= depuis le tiers)
	 */
	RETOURNE("retourné", "retournée");

	private final String descriptionM;
	private final String descriptionF;

	TypeEtatDocumentFiscal(String descriptionM, String descriptionF) {
		this.descriptionM = descriptionM;
		this.descriptionF = descriptionF;
	}

	/**
	 * @return le type en français accordé au féminin.
	 */
	public String descriptionF() {
		return descriptionF;
	}

	/**
	 * @return le type en français accordé au masculin.
	 */
	public String descriptionM() {
		return descriptionM;
	}
}
