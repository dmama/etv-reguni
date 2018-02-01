package ch.vd.unireg.editique;

/**
 * Différents types d'impressions associés aux documents éditique
 */
public enum TypeImpressionEditique {

	/**
	 * Impression directe, réponse attendue par retour du courrier
	 */
	DIRECT("D"),

	/**
	 * Impression différée sans réponse attendue, envoi direct au destinataire
	 */
	BATCH("B");

	private final String code;

	TypeImpressionEditique(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
