package ch.vd.uniregctb.editique;

public enum TypeDocumentEditique {

	DI_ORDINAIRE_COMPLETE("RGPI0801", "impressionDI"),
	DI_ORDINAIRE_VAUDTAX("RGPI0802", "impressionDI"),
	DI_HC_IMMEUBLE("RGPI0803", "impressionDI"),
	DI_DEPENSE("RGPI0804", "impressionDI"),
	SOMMATION_DI("RGPS0801", "sommationDI"),
	LR("ISPL0801", "impressionLR"),
	SOMMATION_LR("ISPS0801", "sommationLR"),
	CHEMISE_TO("RGPT0801", "impressionChemiseTO"),
	CONFIRMATION_DELAI("RGPC0801", "impressionConfirmationDelai"),
	BORDEREAU_MVT_DOSSIER("RGPB0801", "impressionBordereauEnvoiMouvementsDossier"),
	FICHE_OUVERTURE_DOSSIER("RGPF0801", "impressionFicheOuvertureDossier");

	private final String codeDocumentEditique;
	private final String contexteImpression;

	/**
	 * @param codeDocumentEditique code du document pour éditique, exemple : RGPI0801 pour les déclarations d'impôt ordinaires complètes
	 * @param contexteImpression valeur à assigner au champ "context" du message de demande d'impression envoyé à l'ESB
	 */
	private TypeDocumentEditique(String codeDocumentEditique, String contexteImpression) {
		this.codeDocumentEditique = codeDocumentEditique;
		this.contexteImpression = contexteImpression;
	}

	public String getCodeDocumentEditique() {
		return codeDocumentEditique;
	}

	public String getContexteImpression() {
		return contexteImpression;
	}
}
