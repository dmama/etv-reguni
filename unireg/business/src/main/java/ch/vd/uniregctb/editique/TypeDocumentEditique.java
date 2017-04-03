package ch.vd.uniregctb.editique;

import org.jetbrains.annotations.Nullable;

public enum TypeDocumentEditique {

	//
	// Autour des déclarations d'impôt des personnes physiques
	//

	DI_ORDINAIRE_COMPLETE("RGPI0801", "380", "impressionDI"),
	DI_ORDINAIRE_VAUDTAX("RGPI0802", "381", "impressionDI"),
	DI_HC_IMMEUBLE("RGPI0803", "382", "impressionDI"),
	DI_DEPENSE("RGPI0804", "383", "impressionDI"),
	SOMMATION_DI("RGPS0801", "385", "sommationDI"),
	CHEMISE_TO("RGPT0801", null, "chemiseTO"),
	CONFIRMATION_DELAI("RGPC0801", "387", "confirmationDelai"),

	//
	// Autour des déclarations d'impôt des personnes morales
	//

	DI_PM("U1P1DIPM", "408", "impressionDI"),
	DI_APM("U1P2DIAP", "409", "impressionDI"),
	SOMMATION_DI_PM("U1P4SOMM", "401", "sommationDI"),
	ACCORD_DELAI_PM("U1P5DLAI", "402", "accordDelai"),
	REFUS_DELAI_PM("U1P6REFU", "403", "refusDelai"),
	RAPPEL("U1P7RAPL", "404", "rappel"),
	SURSIS("U1P8ADAS", "405", "sursis"),                 // ADAS = Accord de Délai Après Sommation

	//
	// Autres documents fiscaux
	//

	LETTRE_BIENVENUE("U1P3BIEN", "400", "lettreBienvenue"),
	DEMANDE_BILAN_FINAL("U1P9BIFI", "411", "demandeBilanFinal"),
	AUTORISATION_RADIATION_RC("U1PAARAD", "412", "autorisationRadiationRC"),
	LETTRE_TYPE_INFO_LIQUIDATION("U1PSLIQU", "413", "lettreTypeInfoLiquidation"),

	//
	// Demandes de dégrèvement
	//

	DEMANDE_DEGREVEMENT_ICI("U1PTLDEG", "414", "demandeDegrevementICI"),
	RAPPEL_DEMANDE_DEGREVEMENT_ICI("U1PURDEG", "415", "rappelDemandeDegrevementICI"),

	//
	// Autour des questionnaires SNC
	//

	QSNC("U1PQQSNC", "407", "impressionQSNC"),
	RAPPEL_SQNC("U1PRQSNC", "406", "rappelQSNC"),

	//
	// Autour des listes récapitulatives IS
	//

	LR("ISPL0801", "350", "impressionLR"),
	SOMMATION_LR("ISPS0801", "355", "sommationLR"),

	//
	// Vieux trucs obsolètes...
	//

	BORDEREAU_MVT_DOSSIER("RGPB0801", null, "bordereauEnvoiMouvementsDossier"),
	FICHE_OUVERTURE_DOSSIER("RGPF0801", null, "ficheOuvertureDossier"),

	//
	// e-facture
	//

	E_FACTURE_ATTENTE_CONTACT("RGPE1201","390","eFactureAttenteContact"),
	E_FACTURE_ATTENTE_SIGNATURE("RGPE1202","391","eFactureAttenteSignature"),

	//
	//Fourre neutre
	//

	FOURRE_NEUTRE("U1POFOUR",null,"fourreNeutre");


	private final String codeDocumentEditique;
	private final String codeDocumentArchivage;
	private final String contexteImpression;

	/**
	 * @param codeDocumentEditique code du document pour éditique, exemple : RGPI0801 pour les déclarations d'impôt ordinaires complètes
	 * @param codeDocumentArchivage code du document pour l'archivage du document, exemple 355 pour les sommations de LR
	 * @param contexteImpression valeur à assigner au champ "context" du message de demande d'impression envoyé à l'ESB
	 */
	TypeDocumentEditique(String codeDocumentEditique, @Nullable String codeDocumentArchivage, String contexteImpression) {
		this.codeDocumentEditique = codeDocumentEditique;
		this.codeDocumentArchivage = codeDocumentArchivage;
		this.contexteImpression = contexteImpression;
	}

	public String getCodeDocumentEditique() {
		return codeDocumentEditique;
	}

	@Nullable
	public String getCodeDocumentArchivage() {
		return codeDocumentArchivage;
	}

	public String getContexteImpression() {
		return contexteImpression;
	}
}
