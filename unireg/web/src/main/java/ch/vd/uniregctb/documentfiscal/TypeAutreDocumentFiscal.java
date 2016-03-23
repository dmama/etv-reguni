package ch.vd.uniregctb.documentfiscal;

/**
 * Types existants des autres documents fiscaux
 */
public enum TypeAutreDocumentFiscal {

	LETTRE_BIENVENUE("Lettre de bienvenue"),
	;

	private final String displayName;

	TypeAutreDocumentFiscal(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
