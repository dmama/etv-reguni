package ch.vd.uniregctb.evenement.docsortant;

/**
 * Les différents codes des types de documents sortants dont il faut publier l'envoi
 */
public enum CodeTypeDocumentSortant {

	ASSUJETTISSEMENT_IBC("UNIREG-ASSUJ-IBC"),
	ASSUJETTISSEMENT_ICI("UNIREG-ASSUJ-ICI"),
	ASSUJETTISSEMENT_IRF("UNIREG-ASSUJ-IRF"),       // TODO à valider
	DECLARATION("UNIREG-DI-CO")
	;

	private final String code;

	CodeTypeDocumentSortant(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
