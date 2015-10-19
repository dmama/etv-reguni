package ch.vd.uniregctb.type;

public enum TypeDocument {

	DECLARATION_IMPOT_COMPLETE_BATCH("déclaration d'impôt ordinaire complète"),
	DECLARATION_IMPOT_COMPLETE_LOCAL("déclaration d'impôt ordinaire complète"),
	DECLARATION_IMPOT_VAUDTAX("déclaration d'impôt ordinaire VaudTax"),
	DECLARATION_IMPOT_DEPENSE("déclaration d'impôt dépense"),
	DECLARATION_IMPOT_HC_IMMEUBLE("déclaration d'impôt hors-canton immeuble"),
	LISTE_RECAPITULATIVE("liste récapitulative"),
	E_FACTURE_ATTENTE_CONTACT("document de demande de contact pour E-facture"),
	E_FACTURE_ATTENTE_SIGNATURE("document de demande de signature pour confirmation E-facture");

	private final String description;

	TypeDocument(String description) {
		this.description = description;
	}

	/**
	 * @return une description textuelle du type de document
	 */
	public String getDescription() {
		return description;
	}
}