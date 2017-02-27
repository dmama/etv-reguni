package ch.vd.uniregctb.evenement.docsortant;

public enum TypeDocumentSortant {

	DEMANDE_BILAN_FINAL(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Demande de bilan final à la PM"),
	AUTORISATION_RADIATION_RC(true, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Autorisation de radiation de la PM au RC"),
	LETTRE_TYPE_INFO_LIQUIDATION(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre type information sur liquidation"),
	QSNC(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Questionnaire SNC"),
	RAPPEL_QSNC(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Rappel de questionnaire SNC"),
	DI_PM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Déclaration d'impôt PM"),
	DI_APM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Déclaration d'impôt APM"),
	ACCORD_DELAI_PM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Accord de délai"),
	REFUS_DELAI_PM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Refus de délai"),
	SURSIS(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Accord de délai après sommation"),
	SOMMATION_DI_ENTREPRISE(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Sommation de DI"),
	LETTRE_BIENVENUE_RC_VD(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue Inscrit RC VD"),
	LETTRE_BIENVENUE_PM_HC_IMMEUBLE(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue PM HC avec immeuble"),
	LETTRE_BIENVENUE_PM_HC_ETABLISSEMENT(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue PM HC avec établissement"),
	LETTRE_BIENENUE_APM(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue APM (non-inscrit RC)"),
	RAPPEL_LETTRE_BIENVENUE(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Rappel de lettre de bienvenue"),
	DEMANDE_DEGREVEMENT_ICI(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_ICI, "Formulaire de demande de dégrèvement ICI"),
	RAPPEL_DEMANDE_DEGREVEMENT_ICI(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_ICI, "Rappel de formulaire de demande de dégrèvement ICI"),

	DI_PP_COMPLETE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt manuscrite"),
	DI_PP_VAUDTAX(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt Vaudtax"),
	DI_PP_DEPENSE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt dépense"),
	DI_PP_HC_IMMEUBLE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt Hors-canton immeuble"),
	DI_PP_ANNEXE_IMMEUBLE(false, CodeTypeDocumentSortant.DECLARATION, "Lettre annexe immeuble"),
	SOMMATION_DI_PP(true, CodeTypeDocumentSortant.DECLARATION, "Sommation de DI"),
	CONFIRMATION_DELAI(false, CodeTypeDocumentSortant.DECLARATION, "Confirmation de délai"),

	LR(false, CodeTypeDocumentSortant.LISTE_RECAPITULATIVE, "Liste récapitulative IS"),
	SOMMATION_LR(true, CodeTypeDocumentSortant.LISTE_RECAPITULATIVE, "Sommation de liste récapitulative IS"),

	E_FACTURE_CONTACT(false, CodeTypeDocumentSortant.EFACTURE, "Document e-facture 'contact'"),
	E_FACTURE_SIGNATURE(false, CodeTypeDocumentSortant.EFACTURE, "Document e-facture 'signature'"),

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
