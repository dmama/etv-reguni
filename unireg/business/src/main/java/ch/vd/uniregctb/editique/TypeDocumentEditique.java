package ch.vd.uniregctb.editique;

import org.jetbrains.annotations.Nullable;

public enum TypeDocumentEditique {

	//
	// Autour des déclarations d'impôt des personnes physiques
	//

	DI_ORDINAIRE_COMPLETE("RGPI0801", null, "impressionDI"),
	DI_ORDINAIRE_VAUDTAX("RGPI0802", null, "impressionDI"),
	DI_HC_IMMEUBLE("RGPI0803", null, "impressionDI"),
	DI_DEPENSE("RGPI0804", null, "impressionDI"),
	SOMMATION_DI("RGPS0801", "385", "sommationDI"),
	CHEMISE_TO("RGPT0801", null, "chemiseTO"),
	CONFIRMATION_DELAI("RGPC0801", "387", "confirmationDelai"),

	//
	// Autour des déclarations d'impôt des personnes morales
	//

	DI_PM("U1P1DIPM", null, "impressionDI"),
	DI_APM("U1P2DIAP", null, "impressionDI"),
	LETTRE_BIENVENUE("U1P3BIEN", null, "lettreBienvenue"),
	SOMMATION_DI_PM("U1P4SOMM", "666", "sommationDI"),          // TODO le code archivage est bidon !
	ACCORD_DELAI_PM("U1P5DLAI", null, "accordDelai"),
	REFUS_DELAI_PM("U1P6REFU", null, "refusDelai"),
	RAPPEL("U1P7RAPL", null, "rappel"),
	SURSIS("U1P8ADAS", null, "sursis"),                 // ADAS = Accord de Délai Après Sommation

	//
	// Autour des listes récapitulatives IS
	//

	LR("ISPL0801", null, "impressionLR"),
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
	E_FACTURE_ATTENTE_SIGNATURE("RGPE1202","391","eFactureAttenteSignature");


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
