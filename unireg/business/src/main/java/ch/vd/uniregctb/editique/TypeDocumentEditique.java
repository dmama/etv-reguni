package ch.vd.uniregctb.editique;

import org.jetbrains.annotations.Nullable;

public enum TypeDocumentEditique {

	DI_ORDINAIRE_COMPLETE("RGPI0801", null, "impressionDI"),
	DI_ORDINAIRE_VAUDTAX("RGPI0802", null, "impressionDI"),
	DI_HC_IMMEUBLE("RGPI0803", null, "impressionDI"),
	DI_DEPENSE("RGPI0804", null, "impressionDI"),
	SOMMATION_DI("RGPS0801", "385", "sommationDI"),
	LR("ISPL0801", null, "impressionLR"),
	SOMMATION_LR("ISPS0801", "355", "sommationLR"),
	CHEMISE_TO("RGPT0801", null, "chemiseTO"),
	CONFIRMATION_DELAI("RGPC0801", "387", "confirmationDelai"),
	BORDEREAU_MVT_DOSSIER("RGPB0801", null, "bordereauEnvoiMouvementsDossier"),
	FICHE_OUVERTURE_DOSSIER("RGPF0801", null, "ficheOuvertureDossier"),
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
	private TypeDocumentEditique(String codeDocumentEditique, @Nullable String codeDocumentArchivage, String contexteImpression) {
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
