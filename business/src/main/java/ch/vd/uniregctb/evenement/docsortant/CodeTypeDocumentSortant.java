package ch.vd.uniregctb.evenement.docsortant;

/**
 * Les différents codes des types de documents sortants dont il faut publier l'envoi
 */
public enum CodeTypeDocumentSortant {

	ASSUJETTISSEMENT_IBC("UNIREG-ASSUJ-IBC"),
	ASSUJETTISSEMENT_ICI("UNIREG-ASSUJ-ICI"),
	DECLARATION_COMPTES("UNIREG-DI-CO"),
	DECLARATION("UNIREG-DI"),
	EFACTURE("UNIREG-EFACTURE"),
	LISTE_RECAPITULATIVE("UNIREG-LR");

	private final String code;

	CodeTypeDocumentSortant(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
