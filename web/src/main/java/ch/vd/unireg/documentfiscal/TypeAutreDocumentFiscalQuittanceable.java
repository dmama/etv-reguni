package ch.vd.uniregctb.documentfiscal;

/**
 * Types existants des autres documents fiscaux quittanceables
 */
public enum TypeAutreDocumentFiscalQuittanceable {

	LETTRE_BIENVENUE("Lettre de bienvenue"),
	;

	private final String displayName;

	TypeAutreDocumentFiscalQuittanceable(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
