package ch.vd.uniregctb.evenement.docsortant;

public enum TypeDocumentSortant {

	DEMANDE_BILAN_FINAL(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Demande de bilan final à la PM"),
	AUTORISATION_RADIATION_RC(true, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Autorisation de radiation de la PM au RC"),
	LETTRE_TYPE_INFO_LIQUIDATION(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre type information sur liquidation"),
	QSNC(false, CodeTypeDocumentSortant.DECLARATION, "Questionnaire SNC"),
	RAPPEL_QSNC(false, CodeTypeDocumentSortant.DECLARATION, "Rappel de questionnaire SNC"),
	DI_PM(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt PM"),
	DI_APM(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt APM"),
	ACCORD_DELAI_PM(false, CodeTypeDocumentSortant.DECLARATION, "Accord de délai"),
	REFUS_DELAI_PM(false, CodeTypeDocumentSortant.DECLARATION, "Refus de délai"),
	SURSIS(false, CodeTypeDocumentSortant.DECLARATION, "Accord de délai après sommation"),
	SOMMATION_DI_ENTREPRISE(false, CodeTypeDocumentSortant.DECLARATION, "Sommation de DI"),
	LETTRE_BIENVENUE_RC_VD(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue Inscrit RC VD"),
	LETTRE_BIENVENUE_PM_HC_IMMEUBLE(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue PM HC avec immeuble"),
	LETTRE_BIENVENUE_PM_HC_ETABLISSEMENT(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue PM HC avec établissement"),
	LETTRE_BIENENUE_APM(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue APM (non-inscrit RC)"),
	RAPPEL_LETTRE_BIENVENUE(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Rappel de lettre de bienvenue"),

	DI_PP_COMPLETE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt manuscrite"),                  // TODO à valider
	DI_PP_VAUDTAX(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt Vaudtax"),                      // TODO à valider
	DI_PP_DEPENSE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt dépense"),                      // TODO à valider
	DI_PP_HC_IMMEUBLE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt Hors-canton immeuble"),     // TODO à valider
	SOMMATION_DI_PP(true, CodeTypeDocumentSortant.DECLARATION, "Sommation de DI"),                                 // TODO à valider
	CONFIRMATION_DELAI(false, CodeTypeDocumentSortant.DECLARATION, "Confirmation de délai"),                       // TODO à valider

	LR(false, CodeTypeDocumentSortant.DECLARATION, "Liste récapitulative IS"),                                                       // TODO à valider
	SOMMATION_LR(true, CodeTypeDocumentSortant.DECLARATION, "Sommation de liste récapitulative IS"),                                 // TODO à valider

	E_FACTURE_CONTACT(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IRF, "Document e-facture 'contact'"),                 // TODO à valider
	E_FACTURE_SIGNATURE(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IRF, "Document e-facture 'signature'"),             // TODO à valider

	;

	private final boolean archivageValeurProbante;
	private final CodeTypeDocumentSortant codeTypeDocumentSortant;
	private final String nomDocument;

	TypeDocumentSortant(boolean archivageValeurProbante, CodeTypeDocumentSortant codeTypeDocumentSortant, String nomDocument) {
		this.archivageValeurProbante = archivageValeurProbante;
		this.codeTypeDocumentSortant = codeTypeDocumentSortant;
		this.nomDocument = nomDocument;
	}

	public boolean isArchivageValeurProbante() {
		return archivageValeurProbante;
	}

	public CodeTypeDocumentSortant getCodeTypeDocumentSortant() {
		return codeTypeDocumentSortant;
	}

	public String getNomDocument() {
		return nomDocument;
	}
}
