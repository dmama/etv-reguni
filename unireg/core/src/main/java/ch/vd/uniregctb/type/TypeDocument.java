package ch.vd.uniregctb.type;

public enum TypeDocument {

	//
	// Déclarations d'impôt pour les contribuables assimilés "Personnes Physiques"
	//

	DECLARATION_IMPOT_COMPLETE_BATCH("déclaration d'impôt ordinaire complète", true),
	DECLARATION_IMPOT_COMPLETE_LOCAL("déclaration d'impôt ordinaire complète"),
	DECLARATION_IMPOT_VAUDTAX("déclaration d'impôt ordinaire VaudTax"),
	DECLARATION_IMPOT_DEPENSE("déclaration d'impôt dépense"),
	DECLARATION_IMPOT_HC_IMMEUBLE("déclaration d'impôt hors-canton immeuble"),

	//
	// Déclarations d'impôt pour les contribuables assimilés "Personnes Morales"
	//

	DECLARATION_IMPOT_PM_BATCH("déclaration d'impôt PM", true),
	DECLARATION_IMPOT_PM_LOCAL("déclaration d'impôt PM"),
	DECLARATION_IMPOT_APM_BATCH("déclaration d'impôt APM", true),
	DECLARATION_IMPOT_APM_LOCAL("déclaration d'impôt APM"),

	//
	// Questionnaire SNC
	//

	QUESTIONNAIRE_SNC("questionnaire SNC"),

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
	private final boolean forBatch;

	TypeDocument(String description) {
		this(description, false);
	}

	TypeDocument(String description, boolean forBatch) {
		this.description = description;
		this.forBatch = forBatch;
	}

	/**
	 * @return une description textuelle du type de document
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return <code>true</code> si le type de document est une spécialisation pour les batchs
	 */
	public boolean isForBatch() {
		return forBatch;
	}
}