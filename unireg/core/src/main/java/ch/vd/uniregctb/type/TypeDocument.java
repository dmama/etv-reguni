package ch.vd.uniregctb.type;

public enum TypeDocument {

	//
	// Déclarations d'impôt pour les contribuables assimilés "Personnes Physiques"
	//

	DECLARATION_IMPOT_COMPLETE_BATCH("déclaration d'impôt ordinaire complète"),
	DECLARATION_IMPOT_COMPLETE_LOCAL("déclaration d'impôt ordinaire complète"),
	DECLARATION_IMPOT_VAUDTAX("déclaration d'impôt ordinaire VaudTax"),
	DECLARATION_IMPOT_DEPENSE("déclaration d'impôt dépense"),
	DECLARATION_IMPOT_HC_IMMEUBLE("déclaration d'impôt hors-canton immeuble"),

	//
	// Déclarations d'impôt pour les contribuables assimilés "Personnes Morales"
	//
	DECLARATION_IMPOT_PM("déclaration d'impôt PM"),
	DECLARATION_IMPOT_APM("déclaration d'impôt APM"),

	//
	// Documents IS
	//

	LISTE_RECAPITULATIVE("liste récapitulative"),

	//
	// E-facture
	//

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