package ch.vd.unireg.evenement.docsortant;

public enum TypeDocumentSortant {

	DEMANDE_BILAN_FINAL(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Demande de bilan final à la PM", true),
	AUTORISATION_RADIATION_RC(true, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Autorisation de radiation de la PM au RC", true),
	LETTRE_TYPE_INFO_LIQUIDATION(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre type information sur liquidation", true),
	QSNC(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Questionnaire SNC", true),
	DUPLICATA_QSNC(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Questionnaire SNC (duplicata)", false),
	RAPPEL_QSNC(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Rappel de questionnaire SNC", true),
	ACCORD_DELAI_QSNC(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Accord de délai Questionnaire SNC", true),
	REFUS_DELAI_QSNC(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Refus de délai Questionnaire SNC", true),
	DI_PM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Déclaration d'impôt PM", true),
	DUPLICATA_DI_PM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Déclaration d'impôt PM (duplicata)", false),
	DI_APM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Déclaration d'impôt APM", true),
	DUPLICATA_DI_APM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Déclaration d'impôt APM (duplicata)", false),
	ACCORD_DELAI_PM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Accord de délai", true),
	REFUS_DELAI_PM(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Refus de délai", true),
	SURSIS(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Accord de délai après sommation", true),
	SOMMATION_DI_ENTREPRISE(false, CodeTypeDocumentSortant.DECLARATION_COMPTES, "Sommation de DI", true),
	LETTRE_BIENVENUE_RC_VD(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue Inscrit RC VD", true),
	DUPLICATA_LETTRE_BIENVENUE_RC_VD(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue Inscrit RC VD (duplicata)", true),
	LETTRE_BIENVENUE_PM_HC_IMMEUBLE(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue PM HC avec immeuble", true),
	DUPLICATA_LETTRE_BIENVENUE_PM_HC_IMMEUBLE(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue PM HC avec immeuble (duplicata)", true),
	LETTRE_BIENVENUE_PM_HC_ETABLISSEMENT(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue PM HC avec établissement", true),
	DUPLICATA_LETTRE_BIENVENUE_PM_HC_ETABLISSEMENT(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue PM HC avec établissement (duplicata)", true),
	LETTRE_BIENENUE_APM(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue APM (non-inscrit RC)", true),
	DUPLICATA_LETTRE_BIENENUE_APM(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Lettre de bienvenue APM (non-inscrit RC) (duplicata)", true),
	RAPPEL_LETTRE_BIENVENUE(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_IBC, "Rappel de lettre de bienvenue", true),
	DEMANDE_DEGREVEMENT_ICI(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_ICI, "Formulaire de demande de dégrèvement ICI", true),
	DUPLICATA_DEMANDE_DEGREVEMENT_ICI(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_ICI, "Formulaire de demande de dégrèvement ICI (duplicata)", true),
	RAPPEL_DEMANDE_DEGREVEMENT_ICI(false, CodeTypeDocumentSortant.ASSUJETTISSEMENT_ICI, "Rappel de formulaire de demande de dégrèvement ICI", true),

	DI_PP_COMPLETE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt manuscrite", true),
	DUPLICATA_DI_PP_COMPLETE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt manuscrite (duplicata)", false),
	DI_PP_VAUDTAX(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt Vaudtax", true),
	DUPLICATA_DI_PP_VAUDTAX(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt Vaudtax (duplicata)", false),
	DI_PP_DEPENSE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt dépense", true),
	DUPLICATA_DI_PP_DEPENSE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt dépense (duplicata)", false),
	DI_PP_HC_IMMEUBLE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt Hors-canton immeuble", true),
	DUPLICATA_DI_PP_HC_IMMEUBLE(false, CodeTypeDocumentSortant.DECLARATION, "Déclaration d'impôt Hors-canton immeuble (duplicata)", false),
	DI_PP_ANNEXE_IMMEUBLE(false, CodeTypeDocumentSortant.DECLARATION, "Annexe immeuble", false),
	SOMMATION_DI_PP(true, CodeTypeDocumentSortant.DECLARATION, "Sommation de DI", true),
	CONFIRMATION_DELAI(false, CodeTypeDocumentSortant.DECLARATION, "Confirmation de délai", true),
	REFUS_DELAI(false, CodeTypeDocumentSortant.DECLARATION, "Refus de délai", true),

	LR(false, CodeTypeDocumentSortant.LISTE_RECAPITULATIVE, "Liste récapitulative IS", true),
	DUPLICATA_LR(false, CodeTypeDocumentSortant.LISTE_RECAPITULATIVE, "Liste récapitulative IS (duplicata)", false),
	SOMMATION_LR(true, CodeTypeDocumentSortant.LISTE_RECAPITULATIVE, "Sommation de liste récapitulative IS", true),

	E_FACTURE_CONTACT(false, CodeTypeDocumentSortant.EFACTURE, "Document e-facture 'contact'", true),
	E_FACTURE_SIGNATURE(false, CodeTypeDocumentSortant.EFACTURE, "Document e-facture 'signature'", true),

	;

	private final boolean archivageValeurProbante;
	private final CodeTypeDocumentSortant codeTypeDocumentSortant;
	private final String nomDocument;
	private final boolean quittanceAnnonceDemandee;

	TypeDocumentSortant(boolean archivageValeurProbante, CodeTypeDocumentSortant codeTypeDocumentSortant, String nomDocument, boolean quittanceAnnonceDemandee) {
		this.archivageValeurProbante = archivageValeurProbante;
		this.codeTypeDocumentSortant = codeTypeDocumentSortant;
		this.nomDocument = nomDocument;
		this.quittanceAnnonceDemandee = quittanceAnnonceDemandee;
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

	public boolean isQuittanceAnnonceDemandee() {
		return quittanceAnnonceDemandee;
	}
}
