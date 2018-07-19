package ch.vd.unireg.declaration.ordinaire;

/**
 * Code de routage pour la personne morale :<BR />
 * <ul>
 * <li>1 : PM Vaudoise</li>
 * <li>2 : APM non exonéré à IBC</li>
 * <li>3 : PM HS (Hors Suisse)</li>
 * <li>4 : PM Holding</li>
 * <li>5 : PM HC (Hors Canton)</li>
 * <li>6 : SNC (société en nom collectif)</li>
 * <li>7 : APM Exonérée à IBC</li>
 * </ul>
 */
public enum EnumCodeRoutageDI {
	PM_VAUDOISE(1, "Pour les entreprises de catégorie \"PM\" avec siège VD dont on exclut les sociétés de base et holding"),
	APM_NON_EXONEREE(2, "Pour les entreprise de catégorie \"APM\" (non exonérées IBC) quel que soit la localisation du siège (VD/HC/HS)"),
	PM_HORS_SUISSE(3, "Pour les entreprises de catégorie \"PM\" avec siège hors Suisse VD HC/(HS) dont on exclut les sociétés de base et holding"),
	PM_HOLDING(4, "pour les entreprise de catégorie \"PM\" quelle que soit la localisation du siège (VD/HC/HS) mais uniquement les sociétés de base et holding"),
	PM_HORS_CANTON(5, "pour les entreprises de catégorie \"PM\" avec siège hors Canton (HC) dont on exclut les sociétés de base et holding"),
	PM_SNC(6, "Pour les entreprises de catégorie PM avec Régime = Société de personne (SNC et SC)"),
	APM_EXONEREE(7, "pour les entreprises de catégorie \"APM\" (Exonérée IBC) quelle que soit la localisation du siège (VD/HC/HS)");

	private final int code;
	private final String description;

	EnumCodeRoutageDI(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public int getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}
}
